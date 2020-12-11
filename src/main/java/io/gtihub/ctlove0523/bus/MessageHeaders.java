package io.gtihub.ctlove0523.bus;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author chentong
 */
public class MessageHeaders implements Map<String, Object>, Serializable {
	/**
	 * message id
	 */
	public static final String ID = "id";

	public static final String TYPE = "type";

	public static final String PAYLOAD_TYPE = "payloadType";

	public static final String SENDER = "sender";

	public static final String RECEIVER = "receiver";

	private static final long serialVersionUID = 3539782977837872373L;

	private Map<String, Object> headers;

	public MessageHeaders(Map<String, Object> headers) {
		this.headers = new HashMap<>(headers);
	}

	public MessageHeaders(MessageHeaders original) {
		this.headers = new HashMap<>((int) ((float) original.headers.size() / 0.75F), 0.75F);
		original.headers.forEach((key, value) -> {
			this.headers.put(key, value);

		});
	}

	public Map<String, Object> getRawHeaders() {
		return this.headers;
	}

	public String getId() {
		return get(ID, String.class);
	}

	public int getType() {
		return get(TYPE, int.class);
	}

	public <T> Class<T> getPayloadType() {
		return (Class<T>) get(PAYLOAD_TYPE, Class.class);
	}

	public String getSender() {
		return get(SENDER, String.class);
	}

	public String getReceiver() {
		return get(RECEIVER, String.class);
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public Object get(Object key) {
		return this.headers.get(key);
	}

	public <T> T get(Object key, Class<T> type) {
		Object value = this.headers.get(key);
		if (value == null) {
			return null;
		}
		if (!type.isAssignableFrom(value.getClass())) {
			throw new IllegalArgumentException("Incorrect type specified for header '" +
					key + "'. Expected [" + type + "] but actual type is [" + value.getClass() + "]");
		}
		return (T) value;
	}

	@Nullable
	@Override
	public Object put(String key, Object value) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	@Override
	public Object remove(Object key) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	@Override
	public void putAll(@NotNull Map<? extends String, ?> m) {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	@Override
	public void clear() {
		throw new UnsupportedOperationException("MessageHeaders is immutable");
	}

	@NotNull
	@Override
	public Set<String> keySet() {
		return Collections.unmodifiableSet(this.headers.keySet());
	}

	@NotNull
	@Override
	public Collection<Object> values() {
		return Collections.unmodifiableCollection(this.headers.values());
	}

	@NotNull
	@Override
	public Set<Entry<String, Object>> entrySet() {
		return Collections.unmodifiableSet(this.headers.entrySet());
	}
}
