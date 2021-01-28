package io.gtihub.ctlove0523.bus.vertx;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import io.github.ctlove0523.commons.Predications;
import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.gtihub.ctlove0523.bus.MessageHandler;
import io.gtihub.ctlove0523.bus.Server;
import io.gtihub.ctlove0523.bus.SimpleMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

public class VertxTcpServer implements Server {
	private static final Logger log = LoggerFactory.getLogger(VertxTcpServer.class);
	private static final Vertx VERTX = Vertx.vertx();
	private final List<MessageHandler> handlers = new LinkedList<>();
	private final ConcurrentMap<String, NetSocket> connectedClients = new ConcurrentHashMap<>();

	private final int port;
	private NetServer server;

	public VertxTcpServer(int port) {
		this.port = port;
	}

	@Override
	public Mono<Boolean> start() {
		this.server = VERTX.createNetServer();
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
				}
			}
		});

		server.connectHandler(new Handler<NetSocket>() {
			@Override
			public void handle(NetSocket socket) {
				String clientHost = socket.remoteAddress().host();
				connectedClients.put(clientHost, socket);
				socket.closeHandler(new Handler<Void>() {
					@Override
					public void handle(Void event) {
						log.info("client close the connection");
						connectedClients.remove(clientHost);
					}
				});
				socket.handler(new Handler<Buffer>() {
					@Override
					public void handle(Buffer event) {
						handleReceivedEvent(event);
					}
				});
			}
		});
		CompletableFuture<Boolean> startResult = new CompletableFuture<>();
		server.listen(port, new Handler<AsyncResult<NetServer>>() {
			@Override
			public void handle(AsyncResult<NetServer> event) {
				if (event.succeeded()) {
					log.info("event bus server start success,listen on {} port", port);
					startResult.complete(true);
				}
				else {
					log.warn("event bus server start failed,cause ", event.cause());
					startResult.completeExceptionally(event.cause());
				}
			}
		});

		return Mono.fromFuture(startResult);
	}

	@Override
	public Mono<Boolean> gracefulShutdown() {
		if (server == null) {
			return Mono.just(true);
		}

		CompletableFuture<Boolean> shutdownResult = new CompletableFuture<>();
		server.close(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					log.info("server graceful shutdown success");
					shutdownResult.complete(true);
				}
				else {
					shutdownResult.completeExceptionally(event.cause());
				}
			}
		});

		return Mono.fromFuture(shutdownResult);
	}

	@Override
	public void addHandler(MessageHandler handler) {
		Predications.notNull(handler, "message handler must not be null");
		this.handlers.add(handler);
	}

	private void handleReceivedEvent(Buffer event) {
		String jsonFormatData = event.toJson().toString();
		SimpleMessage message = JacksonUtil.json2Object(jsonFormatData, SimpleMessage.class);
		handlers.forEach(new Consumer<MessageHandler>() {
			@Override
			public void accept(MessageHandler handler) {
				handler.handleMessage(message);
			}
		});
	}
}
