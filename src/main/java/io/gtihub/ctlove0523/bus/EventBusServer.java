package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusServer {
	private static final Logger log = LoggerFactory.getLogger(EventBusServer.class);
	private final Vertx vertx = Vertx.vertx();
	private LocalEventBus localEventBus;
	private final AtomicBoolean stared = new AtomicBoolean(false);
	private int port;
	private final Map<String, NetSocket> clientConnections = new HashMap<>();
	private final Map<String, BroadcastEvent> needAckEvents = new HashMap<>();


	private void handleReceivedEvent(Buffer data, String clientHost) {
		String jsonFormatData = data.toJson().toString();
		BroadcastEvent broadcastEvent = JacksonUtil.json2Object(jsonFormatData, BroadcastEvent.class);
		if (validBroadcastEvent(broadcastEvent)) {
			Object localEvent = JacksonUtil.json2Object(broadcastEvent.getBody(), broadcastEvent.getBodyClass());
			localEventBus.post(localEvent);
			acknowledge(broadcastEvent, clientConnections.get(clientHost));
		}
	}

	/**
	 * todo: need ack ack event?
	 */
	private void acknowledge(BroadcastEvent broadcastEvent, NetSocket connection) {
		needAckEvents.put(broadcastEvent.getId(), broadcastEvent);
		BroadcastEvent ackEvent = new BroadcastEvent();
		ackEvent.setType(1);
		ackEvent.setId(broadcastEvent.getId());
		connection.write(JacksonUtil.object2Json(ackEvent), new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					log.trace("{} event ack success",broadcastEvent.getId());
					needAckEvents.remove(broadcastEvent.getId());
				}
			}
		});
	}

	/**
	 * 校验broadcast event是否合法
	 * @param broadcastEvent 广播事件
	 */
	private boolean validBroadcastEvent(BroadcastEvent broadcastEvent) {
		return true;
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