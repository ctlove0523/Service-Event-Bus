package io.gtihub.ctlove0523.bus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.github.ctlove0523.commons.Predications;
import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.github.ctlove0523.discovery.api.Instance;
import io.github.ctlove0523.discovery.api.InstanceAddress;
import io.github.ctlove0523.discovery.api.Order;
import io.github.ctlove0523.discovery.api.ServiceResolver;
import io.gtihub.ctlove0523.bus.repository.WaitAckEventRepository;
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
	private WaitAckEventRepository repository;

	/**
	 * 等待发送的事件
	 */
	private final Map<String, List<BroadcastEvent>> waitSendEvents = new HashMap<>();

	private final ScheduledExecutorService workers = Executors.newScheduledThreadPool(3);


	public EventBusSender(String serviceDomainName, LocalEventBus localEventBus) {
		this(serviceDomainName, DEFAULT_PORT, localEventBus, null);
	}

	public EventBusSender(String serviceDomainName, int receiverPort, LocalEventBus localEventBus,
			WaitAckEventRepository repository) {
		this.serviceDomainName = serviceDomainName;
		this.serviceResolver = new ServiceResolver() {
			@Override
			public List<Instance> resolve(String s) {
				InstanceAddress address = new InstanceAddress("192.168.2.103");
				Instance instance = new Instance(address);
				return Collections.singletonList(instance);
			}

			@Override
			public int getOrder() {
				return 0;
			}
		};
		this.receiverPort = receiverPort;
		this.localEventBus = localEventBus;
		this.repository = repository;
		initReceivers();
		workers.scheduleWithFixedDelay(this::initReceivers, 0L, 10L, TimeUnit.SECONDS);
		workers.scheduleWithFixedDelay(this::reSend, 0L, 5L, TimeUnit.SECONDS);
		workers.scheduleWithFixedDelay(this::reBroadcast, 0L, 5L, TimeUnit.SECONDS);
	}

	private ServiceResolver findServiceResolver() {
		List<ServiceResolver> resolvers = new ArrayList<>();
		ServiceLoader.load(ServiceResolver.class).forEach(new Consumer<ServiceResolver>() {
			@Override
			public void accept(ServiceResolver serviceResolver) {
				resolvers.add(serviceResolver);
			}
		});
		Predications.notEmpty(resolvers, "must has one implement of ServiceResolver");

		resolvers.sort(Comparator.comparingInt(Order::getOrder));

		return resolvers.get(0);
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
			public void accept(String receiverHost, NetSocket socket) {
				// 存储已经发送但是没有被确认的事件
				saveWaitAckEvents(receiverHost, broadcastEvent);

				// 存储已经发送但是还没有发送成功的事件
				List<BroadcastEvent> waitSendEventList = waitSendEvents.get(receiverHost);
				if (waitSendEventList == null) {
					waitSendEventList = new LinkedList<>();
				}
				waitSendEventList.add(broadcastEvent);
				waitSendEvents.put(receiverHost, waitSendEventList);

				// 通过socket广播事件
				log.info("send content {}", JacksonUtil.object2Json(broadcastEvent));
				socket.write(JacksonUtil.object2Json(broadcastEvent), new Handler<AsyncResult<Void>>() {
					@Override
					public void handle(AsyncResult<Void> event) {
						if (event.succeeded()) {
							waitSendEvents.get(receiverHost).remove(broadcastEvent);
						}
					}
				});
			}
		});
	}

	private void saveWaitAckEvents(String receiverHost, BroadcastEvent event) {
		String savedKey = receiverHost + "&" + event.getId();
		log.info("saveWaitAckEvents: key = {}", savedKey);
		event.setReceiverHost(receiverHost);
		repository.save(savedKey, event);
	}


	public void initReceivers() {
		List<Instance> instances = serviceResolver.resolve(serviceDomainName);
		instances.stream()
				.map(instance -> instance.getAddress().getIpv4())
				.filter(host -> !host.equals(IpUtils.getCurrentListenIp()))
				.filter(s -> !receivers.containsKey(s))
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
		String deleteKey = receiverHost + "&" + broadcastEvent.getId();
		log.info("saveWaitAckEvents: key = {}", deleteKey);
		repository.delete(deleteKey);
	}

	/**
	 * 对通过socket发送失败的事件进行重试。发功成功只是指事件通过socket写入成功，但是事件并不一定能被接收者
	 * 接收到
	 */
	private void reSend() {
		Map<String, List<BroadcastEvent>> snapshot = new HashMap<>(waitSendEvents);
		snapshot.forEach((receiverHost, broadcastEvents) -> broadcastEvents.stream()
				.filter(broadcastEvent -> !broadcastEvent.eventIsDeat())
				.forEach(broadcastEvent -> {
					receivers.get(receiverHost)
							.write(JacksonUtil.object2Json(broadcastEvent),
									event -> {
										if (event.succeeded()) {
											waitSendEvents.get(receiverHost).remove(broadcastEvent);
										}
									});
				}));

	}

	/**
	 * 重发没有收到确认的事件,从repository这种获取
	 */
	private void reBroadcast() {
		repository.findAll()
				.filter(event -> !event.eventIsDeat())
				.subscribe(event -> receivers.get(event.getReceiverHost())
						.write(JacksonUtil.object2Json(event)));
	}
}
