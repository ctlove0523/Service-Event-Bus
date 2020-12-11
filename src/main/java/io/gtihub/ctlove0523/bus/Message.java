package io.gtihub.ctlove0523.bus;

public interface Message<T> {

	T getPayload();

	MessageHeaders getHeaders();
}
