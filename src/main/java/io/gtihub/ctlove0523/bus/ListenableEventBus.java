package io.gtihub.ctlove0523.bus;

import java.util.concurrent.ExecutorService;

public interface ListenableEventBus {

	void post(String topic,Object event);

	void registerListener(String topic, Listener listener);

	void registerListener(String topic, Listener listener, ExecutorService executor);

}
