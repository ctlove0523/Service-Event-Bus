package io.gtihub.ctlove0523.bus;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;

/**
 * event bus in single JVM，there have many implements like guava event bus
 * and greenrobot‘s eventbus. we define an interface for purpose not limit user
 * choose implement.
 */
public interface LocalEventBus {

	static LocalEventBus localEventBus(){
		EventBus eventBus = EventBus.getDefault();
		return new LocalEventBus() {
			@Override
			public void register(@NotNull Object subscriber) {
				eventBus.register(subscriber);
			}

			@Override
			public void post(@NotNull Object event) {
				eventBus.post(event);
			}
		};
	}

	void register(@NotNull Object subscriber);

	void post(@NotNull Object event);
}
