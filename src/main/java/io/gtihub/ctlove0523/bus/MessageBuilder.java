package io.gtihub.ctlove0523.bus;

import io.github.ctlove0523.commons.Predications;

public class MessageBuilder<T> {

	private T payload;
	private MessageHeaders headers;

	MessageBuilder(Message<T> message) {
		this.payload = message.getPayload();
		this.headers = new MessageHeaders();
	}

	MessageBuilder(T payload, MessageHeaders headers) {
		this.payload = payload;
		this.headers = headers;
	}

	public MessageBuilder<T> setHeaders(MessageHeaders accessor) {
		Predications.notNull(accessor, "MessageHeaderAccessor must not be null");
		this.headers = accessor;
		return this;
	}

	/**
	 * Set the value for the given header name. If the provided value is {@code null},
	 * the header will be removed.
	 */
	public MessageBuilder<T> setHeader(String headerName, Object headerValue) {
		this.headers.setHeader(headerName, headerValue);
		return this;
	}

	/**
	 * Set the value for the given header name only if the header name is not already
	 * associated with a value.
	 */
	public MessageBuilder<T> setHeaderIfAbsent(String headerName, Object headerValue) {
		this.headers.setHeaderIfAbsent(headerName, headerValue);
		return this;
	}


	public MessageBuilder<T> removeHeaders(String... headers) {
		for (String header : headers) {
			this.headers.removeHeader(header);
		}
		return this;
	}

	public MessageBuilder<T> removeHeader(String headerName) {
		this.headers.removeHeader(headerName);
		return this;
	}


	public static <T> MessageBuilder<T> fromMessage(Message<T> message) {
		return new MessageBuilder<>(message);
	}

	public static <T> MessageBuilder<T> withPayload(T payload) {
		return new MessageBuilder<>(payload, new MessageHeaders());
	}

	public static <T> Message<T> createMessage(T payload, MessageHeaders headers) {
		return new SimpleMessage<>(payload, headers);
	}
}
