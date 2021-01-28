package io.gtihub.ctlove0523.bus;

import reactor.core.publisher.Mono;

public interface Server {

	Mono<Boolean> start();

	Mono<Boolean> gracefulShutdown();

	void addHandler(MessageHandler handler);
}
