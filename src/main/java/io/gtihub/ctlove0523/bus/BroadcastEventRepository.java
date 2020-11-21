package io.gtihub.ctlove0523.bus;

import java.util.Iterator;

public interface BroadcastEventRepository {

	void save(BroadcastEvent event);

	void findOne(String id);

	void deleteOne(String id);

	Iterator<BroadcastEvent> findMany(Iterator<String> ids);

	Iterator<BroadcastEvent> findAll();

	void deleteMany(Iterator<String> ids);
}
