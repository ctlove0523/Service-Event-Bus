package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.ctlove0523.commons.network.Ips;
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
				.filter(new Predicate<Instance>() {
					@Override
					public boolean test(Instance instance) {
						return !instance.getAddress().getIpv4().equals(Ips.getIpByNetworkInterfaceName("eth0").getHostAddress());
					}
				}).forEach(new Consumer<Instance>() {
			@Override
			public void accept(Instance instance) {
				NetClient receiver = vertx.createNetClient();
				receiver.connect(receiverPort, instance.getAddress().getIpv4(), new Handler<AsyncResult<NetSocket>>() {
					@Override
					public void handle(AsyncResult<NetSocket> event) {
						if (event.succeeded()) {
							log.info("connect to receiver {} success", instance.getAddress().getIpv4());
							receivers.put(instance.getAddress().getIpv4(), event.result());
							event.result().handler(new Handler<Buffer>() {
								@Override
								public void handle(Buffer event) {

								}
							});
						}
						else {
							log.warn("connect to receiver {} failed", instance.getAddress().getIpv4());
							receivers.remove(instance.getAddress().getIpv4());
						}
					}
				});
			}
		});
	}

	private void acknowledgeReceiverAck(Buffer data) {

	}
}
