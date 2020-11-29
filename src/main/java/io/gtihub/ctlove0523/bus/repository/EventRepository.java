package io.gtihub.ctlove0523.bus.repository;

import java.util.List;

import io.gtihub.ctlove0523.bus.BroadcastEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface EventRepository {

	void save(String key, BroadcastEvent event);

	Mono<BroadcastEvent> find(String key);

	void delete(String key);

	Flux<BroadcastEvent> findMany(List<String> keys);

	Flux<BroadcastEvent> findAll();

	void deleteMany(List<String> keys);
}
