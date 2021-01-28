package io.gtihub.ctlove0523.bus.vertx;

import java.util.concurrent.CompletableFuture;

import io.github.ctlove0523.commons.serialization.JacksonUtil;
import io.gtihub.ctlove0523.bus.Client;
import io.gtihub.ctlove0523.bus.Message;
import io.gtihub.ctlove0523.bus.SimpleMessage;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import reactor.core.publisher.Mono;

public class VertxTcpClient implements Client {
	private static final Vertx VERTX = Vertx.vertx();
	private final String serverAddress;
	private final int serverPort;
	private NetClient client;
	private NetSocket socket;

	public VertxTcpClient(String serverAddress, int serverPort) {
		this.serverAddress = serverAddress;
		this.serverPort = serverPort;
	}


	private void init() {
		NetClientOptions options = new NetClientOptions();
		options.setReusePort(true);
		options.setReconnectInterval(2 * 1000L);
		options.setReconnectAttempts(3);
		this.client = VERTX.createNetClient(options);
	}

	@Override
	public Mono<Boolean> connect() {
		if (client == null) {
			init();
		}

		CompletableFuture<Boolean> connectResult = new CompletableFuture<>();

		client.connect(serverPort, serverAddress, new Handler<AsyncResult<NetSocket>>() {
			@Override
			public void handle(AsyncResult<NetSocket> event) {
				if (event.succeeded()) {
					System.out.println("connect success");
					socket = event.result();
					connectResult.complete(true);
				}
				else {
					System.out.println("connect failed");
					connectResult.complete(false);
				}
			}
		});

		return Mono.fromFuture(connectResult);
	}

	@Override
	public Mono<Boolean> disConnect() {
		CompletableFuture<Boolean> disConnectResult = new CompletableFuture<>();
		socket.close(new Handler<AsyncResult<Void>>() {
			@Override
			public void handle(AsyncResult<Void> event) {
				if (event.succeeded()) {
					System.out.println("close success");
					disConnectResult.complete(true);
				}
				else {
					disConnectResult.complete(false);
				}

			}
		});
		client.close();

		return Mono.fromFuture(disConnectResult);
	}

	@Override
	public Mono<Boolean> sendMessage(Message<?> message, long timeOut) {
		CompletableFuture<Boolean> future = new CompletableFuture<>();
		if (message instanceof SimpleMessage) {
			SimpleMessage<?> simpleMessage = (SimpleMessage<?>) message;
			String stringMessage = JacksonUtil.object2Json(simpleMessage);
			System.out.println(stringMessage);
			socket.write(JacksonUtil.object2Json(simpleMessage), new Handler<AsyncResult<Void>>() {
				@Override
				public void handle(AsyncResult<Void> event) {
					if (event.succeeded()) {
						future.complete(true);
					}
					else {
						future.completeExceptionally(event.cause());
					}
				}
			});
		}
		return Mono.fromFuture(future);
	}
}
