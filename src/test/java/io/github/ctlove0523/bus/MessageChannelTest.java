package io.github.ctlove0523.bus;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import io.gtihub.ctlove0523.bus.Message;
import io.gtihub.ctlove0523.bus.MessageBuilder;
import io.gtihub.ctlove0523.bus.vertx.VertxTcpClient;
import reactor.core.publisher.Mono;

public class MessageChannelTest {
	public static void main(String[] args) throws Exception {
		VertxTcpClient client = new VertxTcpClient("localhost", 5432);
		Mono<Boolean> connect = client.connect();

		connect.block();
		Message<String> message = MessageBuilder.withPayload("hello server").build();
		client.sendMessage(message).doOnSuccess(new Consumer<Boolean>() {
			@Override
			public void accept(Boolean aBoolean) {
				System.out.println("client send message success");
			}
		});

		TimeUnit.SECONDS.sleep(10);
	}
}
