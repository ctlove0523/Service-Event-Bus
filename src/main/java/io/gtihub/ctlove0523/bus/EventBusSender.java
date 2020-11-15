package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
	private final Vertx vertx = Vertx.vertx();
	private String serviceDomainName;
	private ServiceResolver serviceResolver;
	private int receiverPort;
	private Map<String, NetSocket> receivers = new HashMap<>();
	private LocalEventBus localEventBus;
	private Map<String, List<BroadcastEvent>> waitAckEvents = new HashMap<>();
	private Map<String, List<BroadcastEvent>> waitSendEvents = new HashMap<>();

	public void post(Object event) {
		BroadcastEvent broadcastEvent = new BroadcastEvent();
		broadcastEvent.setId(UUID.randomUUID().toString());
		broadcastEvent.setType(0);
		broadcastEvent.setSenderHost(IpUtils.getCurrentListenIp());
		broadcastEvent.setBody(JacksonUtil.object2Json(event));
		broadcastEvent.setBodyClass(event.getClass());

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
				.map(new Function<Instance, String>() {
					@Override
					public String apply(Instance instance) {
						return instance.getAddress().getIpv4();
					}
				})
				.filter(new Predicate<String>() {
					@Override
					public boolean test(String host) {
						return !host.equals(IpUtils.getCurrentListenIp());
					}
				}).forEach(new Consumer<String>() {
			@Override
			public void accept(String host) {
				NetClient receiver = vertx.createNetClient();
				receiver.connect(receiverPort, host, new Handler<AsyncResult<NetSocket>>() {
					@Override
					public void handle(AsyncResult<NetSocket> event) {
						if (event.succeeded()) {
							log.info("connect to receiver {} success", host);
							receivers.put(host, event.result());
							event.result().handler(new Handler<Buffer>() {
								@Override
								public void handle(Buffer event) {
									acknowledgeReceiverAck(event);
								}
							});
						}
						else {
							log.warn("connect to receiver {} failed", host);
							receivers.remove(host);
						}
					}
				});
			}
		});
	}

	private void acknowledgeReceiverAck(Buffer data) {
		String jsonFormatData = data.toJson().toString();
		BroadcastEvent broadcastEvent = JacksonUtil.json2Object(jsonFormatData, BroadcastEvent.class);
		String receiverHost = broadcastEvent.getReceiverHost();
		waitAckEvents.get(receiverHost).remove(broadcastEvent);
	}
}
