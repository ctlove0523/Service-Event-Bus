package io.gtihub.ctlove0523.bus;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

public class DefaultListenableEventBus implements ListenableEventBus {

	/**
	 * 一个类型的事件所有订阅者
	 */
	private ConcurrentMap<String, List<Listener>> eventToListeners = new ConcurrentHashMap<>();

	private ConcurrentMap<String, ExecutorService> executors = new ConcurrentHashMap<>();

	@Override
	public void post(String topic, Object event) {
		List<Listener> listeners = eventToListeners.get(topic);
		if (listeners == null || listeners.isEmpty()) {
			return;
		}

		listeners.forEach(new Consumer<Listener>() {
			@Override
			public void accept(Listener listener) {
				ExecutorService executor = executors.get(listener);
				if (executor != null) {
					executor.submit(new Runnable() {
						@Override
						public void run() {
							listener.listen(event);
						}
					});
				}
				else {
					listener.listen(event);
				}
			}
		});
	}

	@Override
	public void registerListener(String topic, Listener listener) {
		List<Listener> listeners = eventToListeners.get(topic);
		if (listeners == null) {
			listeners = new LinkedList<>();
		}
		listeners.add(listener);

		eventToListeners.put(topic, listeners);
	}

	@Override
	public void registerListener(String topic, Listener listener, ExecutorService executor) {
		registerListener(topic, listener);

		executors.put(listener.getClass().getName(), executor);
	}
}
