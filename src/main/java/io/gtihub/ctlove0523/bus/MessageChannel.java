package io.gtihub.ctlove0523.bus;

import reactor.core.publisher.Mono;

public interface MessageChannel {
	long INDEFINITE_TIME_OUT = -1L;

	default Mono<Boolean> sendMessage(Message<?> message) {
		return sendMessage(message, INDEFINITE_TIME_OUT);
	}

	Mono<Boolean> sendMessage(Message<?> message, long timeOut);
}
