package io.gtihub.ctlove0523.bus.repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.gtihub.ctlove0523.bus.BroadcastEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryWaitSendAckEventRepository implements WaitSendAckEventRepository {
	private final ConcurrentMap<String, BroadcastEvent> repository = new ConcurrentHashMap<>();

	@Override
	public void save(String key, BroadcastEvent event) {
		repository.put(key, event);
	}

	@Override
	public Mono<BroadcastEvent> find(String key) {
		return Mono.justOrEmpty(repository.get(key));
	}

	@Override
	public void delete(String key) {
		repository.remove(key);
	}

	@Override
	public Flux<BroadcastEvent> findMany(List<String> keys) {
		return Flux.from(s -> {
			keys.forEach(key -> s.onNext(repository.get(key)));
			s.onComplete();
		});
	}

	@Override
	public Flux<BroadcastEvent> findAll() {
		return Flux.fromIterable(repository.values());
	}

	@Override
	public void deleteMany(List<String> keys) {
		keys.forEach(repository::remove);
	}
}
