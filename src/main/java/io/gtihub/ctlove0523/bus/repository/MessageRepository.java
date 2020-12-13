package io.gtihub.ctlove0523.bus.repository;

import java.util.List;

import io.gtihub.ctlove0523.bus.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessageRepository {

	<T> void save(String key, Message<T> event);

	<T> Mono<Message<T>> find(String key);

	void delete(String key);

	<T> Flux<Message<T>> findMany(List<String> keys);

	<T> Flux<Message<T>> findAll();

	void deleteMany(List<String> keys);
}
