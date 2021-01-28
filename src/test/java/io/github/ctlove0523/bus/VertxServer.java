package io.github.ctlove0523.bus;

import io.gtihub.ctlove0523.bus.Message;
import io.gtihub.ctlove0523.bus.MessageHandler;
import io.gtihub.ctlove0523.bus.Server;
import io.gtihub.ctlove0523.bus.vertx.VertxTcpServer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetServer;
import io.vertx.core.net.NetSocket;

public class VertxServer {
	public static void main(String[] args) {
		Server server = new VertxTcpServer(5432);
		server.addHandler(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) {
				System.out.println(message.getPayload());
			}
		});
		server.start().block();
	}
}
