package io.gtihub.ctlove0523.bus.repository;

import java.util.Iterator;

import io.gtihub.ctlove0523.bus.BroadcastEvent;

public interface EventRepository {

	void save(String key, BroadcastEvent event);

	void findOne(String key);

	void deleteOne(String key);

	Iterator<BroadcastEvent> findMany(Iterator<String> keys);

	Iterator<BroadcastEvent> findAll();

	void deleteMany(Iterator<String> keys);
}
