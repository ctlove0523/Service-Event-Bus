package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.github.ctlove0523.discovery.api.Instance;
import io.github.ctlove0523.discovery.api.ServiceResolver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusSender {
	private static final Logger log = LoggerFactory.getLogger(EventBusSender.class);
	private static final int DEFAULT_SURVIVAL_TIME = 30 * 1000;
	private static final int DEFAULT_PORT = 7160;
	private final Vertx vertx = Vertx.vertx();
	private final String serviceDomainName;
	private final ServiceResolver serviceResolver;
	private final int receiverPort;
	private final Map<String, NetSocket> receivers = new HashMap<>();
	private final LocalEventBus localEventBus;

	/**
	 * 等待发送的事件
	 */
	private final Map<String, List<BroadcastEvent>> waitSendEvents = new HashMap<>();

	/**
	 * 等待接收者确认的事件，key为接收者的唯一标识
	 */
	private final Map<String, List<BroadcastEvent>> waitAckEvents = new HashMap<>();

	private ScheduledExecutorService workers = Executors.newScheduledThreadPool(3);


	public EventBusSender(String serviceDomainName, ServiceResolver serviceResolver, LocalEventBus localEventBus) {
		this(serviceDomainName, serviceResolver, DEFAULT_PORT, localEventBus);
	}

	public EventBusSender(String serviceDomainName, ServiceResolver serviceResolver, int receiverPort, LocalEventBus localEventBus) {
		this.serviceDomainName = serviceDomainName;
		this.serviceResolver = serviceResolver;
		this.receiverPort = receiverPort;
		this.localEventBus = localEventBus;
		initReceivers();
		workers.scheduleWithFixedDelay(this::initReceivers, 0L, 10L, TimeUnit.SECONDS);
		workers.scheduleWithFixedDelay(this::reSend, 0L, 5L, TimeUnit.SECONDS);
		workers.scheduleWithFixedDelay(this::reBroadcast, 0L, 5L, TimeUnit.SECONDS);
	}

	public void post(Object event) {
		post(event, DEFAULT_SURVIVAL_TIME);
	}

	public void post(Object event, int survivalTime) {
		Map<String, Object> headers = new HashMap<>(3);
		headers.put(BroadcastEventHeaderKeys.BIRTHDAY, System.currentTimeMillis());
		headers.put(BroadcastEventHeaderKeys.SURVIVAL_TIME, survivalTime);
		BroadcastEvent broadcastEvent = new BroadcastEvent();
		broadcastEvent.setId(UUID.randomUUID().toString());
		broadcastEvent.setType(0);
		broadcastEvent.setSenderHost(IpUtils.getCurrentListenIp());
		broadcastEvent.setBody(JacksonUtil.object2Json(event));
		broadcastEvent.setBodyClass(event.getClass());
		broadcastEvent.setHeaders(headers);

		localEventBus.post(event);
		receivers.forEach(new BiConsumer<String, NetSocket>() {
			@Override
			public void accept(String s, NetSocket socket) {
				// 存储已经发送但是没有被确认的事件
				List<BroadcastEvent> broadcastEventList = waitAckEvents.get(s);
				if (broadcastEventList == null) {
					broadcastEventList = new LinkedList<>();
				}
				broadcastEventList.add(broadcastEvent);
				waitAckEvents.put(s, broadcastEventList);

				// 存储已经发送但是还没有发送成功的事件
				List<BroadcastEvent> waitSendEventList = waitSendEvents.get(s);
				if (waitSendEventList == null) {
					waitSendEventList = new LinkedList<>();
				}
				waitSendEventList.add(broadcastEvent);
				waitSendEvents.put(s, waitSendEventList);

				// 通过socket广播事件
				socket.write(JacksonUtil.object2Json(broadcastEvent), new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.succeeded()) {
							waitSendEvents.get(s).remove(broadcastEvent);
						}
					}
				});
			}
		});
	}


	public void initReceivers() {
		List<Instance> instances = serviceResolver.resolve(serviceDomainName);
		instances.stream()
				.map(instance -> instance.getAddress().getIpv4())
				.filter(host -> !host.equals(IpUtils.getCurrentListenIp()))
				.forEach(host -> {
					NetClient receiver = vertx.createNetClient();
					receiver.connect(receiverPort, host, event -> {
						if (event.succeeded()) {
							log.info("connect to receiver {} success", host);
							receivers.put(host, event.result());
							// 处理来自接收者的响应
							event.result().handler(this::acknowledgeReceiverAck);
						}
						else {
							log.warn("connect to receiver {} failed", host);
							receivers.remove(host);
						}
					});
				});
	}

	private void acknowledgeReceiverAck(Buffer data) {
		String jsonFormatData = data.toJson().toString();
		BroadcastEvent broadcastEvent = JacksonUtil.json2Object(jsonFormatData, BroadcastEvent.class);
		String receiverHost = broadcastEvent.getReceiverHost();
		waitAckEvents.get(receiverHost).remove(broadcastEvent);
	}

	private void reSend() {
		Map<String, List<BroadcastEvent>> snapshot = new HashMap<>(waitSendEvents);
		snapshot.forEach((s, broadcastEvents) -> broadcastEvents.forEach(broadcastEvent -> receivers.get(s).write(JacksonUtil.object2Json(broadcastEvent), event -> {
			if (event.succeeded()) {
				waitSendEvents.get(s).remove(broadcastEvent);
			}
		})));
	}

	private void reBroadcast() {
		Map<String, List<BroadcastEvent>> snapshot = new HashMap<>(waitAckEvents);

		snapshot.forEach(new BiConsumer<String, List<BroadcastEvent>>() {
			@Override
			public void accept(String s, List<BroadcastEvent> broadcastEvents) {
				broadcastEvents.forEach(new Consumer<BroadcastEvent>() {
					@Override
					public void accept(BroadcastEvent broadcastEvent) {
						receivers.get(s).write(JacksonUtil.object2Json(broadcastEvent));
					}
				});
			}
		});
	}
}
