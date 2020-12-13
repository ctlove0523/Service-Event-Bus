package io.gtihub.ctlove0523.bus.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import io.gtihub.ctlove0523.bus.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class InMemoryWaitSendAckMessageRepository implements WaitSendAckMessageRepository {
	private final ConcurrentMap<String, Message<?>> repository = new ConcurrentHashMap<>();

	@Override
	public <T> void save(String key, Message<T> event) {
		repository.put(key, event);
	}

	@Override
	public <T> Mono<Message<T>> find(String key) {
		return Mono.justOrEmpty((Message<T>) repository.get(key));
	}

	@Override
	public void delete(String key) {
		repository.remove(key);
	}

	@Override
	public <T> Flux<Message<T>> findMany(List<String> keys) {
		return Flux.from(s -> {
			keys.forEach(key -> s.onNext((Message<T>) repository.get(key)));
			s.onComplete();
		});
	}

	@Override
	public <T> Flux<Message<T>> findAll() {
		List<Message<T>> messages = new ArrayList<>(repository.size());
		repository.values().forEach(new Consumer<Message<?>>() {
			@Override
			public void accept(Message<?> message) {
				messages.add((Message<T>) message);
			}
		});
		return Flux.fromIterable(messages);
	}

	@Override
	public void deleteMany(List<String> keys) {
		keys.forEach(repository::remove);
	}
}
