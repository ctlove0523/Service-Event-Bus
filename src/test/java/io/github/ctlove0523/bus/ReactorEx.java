package io.github.ctlove0523.bus;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.junit.Test;
import reactor.cache.CacheMono;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Signal;
import reactor.util.context.Context;

public class ReactorEx {
	@Test
	public void test() {
		AtomicReference<Context> storeRef = new AtomicReference<>(Context.empty());

		Mono<Integer> cachedMono = CacheMono
				.lookup(k -> {
							return Mono.justOrEmpty(storeRef.get().<Integer>getOrEmpty(k))
									.map(Signal::next);
						},
						"key")
				.onCacheMissResume(Mono.just(123))
				.andWriteWith((String k, Signal<? extends Integer> sig) -> {
					return Mono.fromRunnable(() -> {
						storeRef.updateAndGet((Context ctx) -> {
							return ctx.put(k, sig.get());
						});
					});
				});

		Mono<Integer> cache = CacheMono
				.lookup(new Function<String, Mono<Signal<? extends Integer>>>() {
					@Override
					public Mono<Signal<? extends Integer>> apply(String o) {
						return null;
					}
				}, "hello")
				.onCacheMissResume(Mono.just(20))
				.andWriteWith(new BiFunction<String, Signal<? extends Integer>, Mono<Void>>() {

					@Override
					public Mono<Void> apply(String s, Signal<? extends Integer> signal) {
						return null;
					}
				});
	}
}
