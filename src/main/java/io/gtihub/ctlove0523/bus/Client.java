package io.gtihub.ctlove0523.bus;

import reactor.core.publisher.Mono;

public interface Client extends MessageChannel {

	Mono<Boolean> connect();

	Mono<Boolean> disConnect();
}
