package io.gtihub.ctlove0523.bus;

import java.io.Serializable;

public class SimpleMessage<T> implements Message<T>, Serializable {
	private static final long serialVersionUID = 301792511685718238L;

	private T payload;

	private MessageHeaders headers;

	SimpleMessage() {
		this.payload = null;
		this.headers = new MessageHeaders();
	}

	SimpleMessage(T payload) {
		this(payload, new MessageHeaders());
	}

	SimpleMessage(T payload, MessageHeaders headers) {
		this.payload = payload;
		this.headers = headers;
	}

	@Override
	public T getPayload() {
		return this.payload;
	}

	@Override
	public MessageHeaders getHeaders() {
		return this.headers;
	}
}
