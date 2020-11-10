package io.gtihub.ctlove0523.bus;

import org.jetbrains.annotations.NotNull;

/**
 * event bus in single JVM，there have many implements like guava event bus
 * and greenrobot‘s eventbus. we define an interface for purpose not limit user
 * choose implement.
 */
public interface LocalEventBus {

	void register(@NotNull Object subscriber);

	void post(@NotNull Object event);
}
