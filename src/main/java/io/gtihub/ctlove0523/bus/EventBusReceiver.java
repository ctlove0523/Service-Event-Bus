package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.gtihub.ctlove0523.bus.repository.WaitSendAckEventRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusReceiver {
	private static final Logger log = LoggerFactory.getLogger(EventBusReceiver.class);
	private final Vertx vertx = Vertx.vertx();
	private final LocalEventBus localEventBus;
	private final AtomicBoolean stared = new AtomicBoolean(false);
	private final int port;
	private final Map<String, NetSocket> clientConnections = new HashMap<>();
	private WaitSendAckEventRepository waitSendAckEventRepository;
	private ScheduledExecutorService worker = Executors.newScheduledThreadPool(1);

	public EventBusReceiver(LocalEventBus localEventBus, int port, WaitSendAckEventRepository repository) {
		this.localEventBus = localEventBus;
		this.port = port;
		this.waitSendAckEventRepository = repository;
		start();
		worker.scheduleWithFixedDelay(() -> {
			waitSendAckEventRepository.findAll()
					.subscribe(new Consumer<BroadcastEvent>() {
						@Override
						public void accept(BroadcastEvent event) {
							acknowledge(event, clientConnections.get(event.getSenderHost()));
						}
					});
		}, 0, 5, TimeUnit.SECONDS);
	}

	public void register(Object subscriber) {
		localEventBus.register(subscriber);
	}

	private void handleReceivedEvent(Buffer data, String clientHost) {
		String jsonFormatData = data.toJson().toString();
		BroadcastEvent broadcastEvent = JacksonUtil.json2Object(jsonFormatData, BroadcastEvent.class);
		if (validBroadcastEvent(broadcastEvent)) {
			Object localEvent = JacksonUtil.json2Object(broadcastEvent.getBody(), broadcastEvent.getBodyClass());
			localEventBus.post(localEvent);
			acknowledge(broadcastEvent, clientConnections.get(clientHost));
		}
		else {
			log.warn("event already dead");
		}
	}

	private void acknowledge(BroadcastEvent broadcastEvent, NetSocket connection) {
		String savedKey = broadcastEvent.getSenderHost() + "&" + broadcastEvent.getId();
		waitSendAckEventRepository.save(savedKey, broadcastEvent);
		BroadcastEvent ackEvent = new BroadcastEvent();
		ackEvent.setType(1);
		ackEvent.setId(broadcastEvent.getId());
		ackEvent.setReceiverHost(IpUtils.getCurrentListenIp());
		connection.write(JacksonUtil.object2Json(ackEvent), new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					log.info("{} event ack success", broadcastEvent.getId());
					waitSendAckEventRepository.delete(savedKey);
				}
			}
		});
	}

	/**
	 * 校验broadcast event是否合法
	 *
	 * @param broadcastEvent 广播事件
	 */
	private boolean validBroadcastEvent(BroadcastEvent broadcastEvent) {
		long deadTime = (long) broadcastEvent.getHeaders().get(BroadcastEventHeaderKeys.BIRTHDAY)
				+ (int) broadcastEvent.getHeaders().get(BroadcastEventHeaderKeys.SURVIVAL_TIME);
		return deadTime > System.currentTimeMillis();
	}

	private void start() {
		NetServer server = vertx.createNetServer();
		server.exceptionHandler(new Handler<Throwable>() {
			@Override
			public void handle(Throwable throwable) {
				log.warn("event bus server get an exception ", throwable);
			}
		});
		server.close(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					log.info("event bus server close success");
					clientConnections.clear();
				}
			}
		});
		server.connectHandler(new Handler<NetSocket>() {
			@Override
			public void handle(NetSocket socket) {
				String clientHost = socket.remoteAddress().host();
				clientConnections.put(clientHost, socket);
				socket.closeHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						log.info("client close the connection");
						clientConnections.remove(clientHost);
					}
				});
				socket.handler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer event) {
						handleReceivedEvent(event, clientHost);
					}
				});
			}
		});
		server.listen(port, new Handler<AsyncResult<NetServer>>() {
			@Override
			public void handle(AsyncResult<NetServer> event) {
				if (event.succeeded()) {
					log.info("event bus server start success,listen on {} port", port);
					stared.compareAndSet(false, true);
				}
				else {
					log.warn("event bus server start failed,cause ", event.cause());
					stared.set(false);
				}
			}
		});
	}
}
