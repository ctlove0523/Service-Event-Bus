package io.github.ctlove0523.bus;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import io.gtihub.ctlove0523.bus.MessageHeaders;
import org.junit.Assert;
import org.junit.Test;
import org.junit.function.ThrowingRunnable;

public class MessageHeadersTests {

	@Test
	public void test_immutable() {
		String errorMessage = "MessageHeaders is immutable";
		Assert.assertThrows(errorMessage, UnsupportedOperationException.class, new ThrowingRunnable() {
			@Override
			public void run() throws Throwable {
				new MessageHeaders().put("key", new Object());
			}
		});

		MessageHeaders messageHeaders = new MessageHeaders();
		messageHeaders.setHeader("key", new Object());
		Assert.assertThrows(errorMessage, UnsupportedOperationException.class, new ThrowingRunnable() {
			@Override
			public void run() throws Throwable {
				messageHeaders.remove("key");
			}
		});

		Assert.assertThrows(errorMessage, UnsupportedOperationException.class, new ThrowingRunnable() {
			@Override
			public void run() throws Throwable {
				messageHeaders.clear();
			}
		});

		Assert.assertThrows(errorMessage, UnsupportedOperationException.class, new ThrowingRunnable() {
			@Override
			public void run() throws Throwable {
				Map<String, Object> headers = new HashMap<>();
				headers.put("all", new Object());
				messageHeaders.putAll(headers);
			}
		});
	}

	@Test
	public void test_implementMethods() {
		MessageHeaders messageHeaders = new MessageHeaders();
		messageHeaders.setHeader(MessageHeaders.ID, "id");
		messageHeaders.setHeader(MessageHeaders.RECEIVER, "receiver");
		messageHeaders.setHeader(MessageHeaders.SENDER, "sender");
		messageHeaders.setHeader(MessageHeaders.TYPE, 1);
		messageHeaders.setHeader(MessageHeaders.PAYLOAD_TYPE, String.class);

		Assert.assertTrue(messageHeaders.keySet()
				.containsAll(Arrays.asList("id", "receiver", "sender", "type", "payloadType")));
		Assert.assertTrue(messageHeaders.values()
				.containsAll(Arrays.asList("id", "receiver", "sender", 1, String.class)));
		Assert.assertTrue(messageHeaders.entrySet().stream().allMatch(new Predicate<Map.Entry<String, Object>>() {
			@Override
			public boolean test(Map.Entry<String, Object> entry) {
				return Arrays.asList("id", "receiver", "sender", "type", "payloadType").contains(entry.getKey())
						&& Arrays.asList("id", "receiver", "sender", 1, String.class).contains(entry.getValue());
			}
		}));
	}

	@Test
	public void test_keyMethods() {
		MessageHeaders messageHeaders = new MessageHeaders();
		messageHeaders.setHeader(MessageHeaders.ID, "id");
		messageHeaders.setHeader(MessageHeaders.RECEIVER, "receiver");
		messageHeaders.setHeader(MessageHeaders.SENDER, "sender");
		messageHeaders.setHeader(MessageHeaders.TYPE, 1);
		messageHeaders.setHeader(MessageHeaders.PAYLOAD_TYPE, String.class);

		Assert.assertEquals("id", messageHeaders.getId());
		Assert.assertEquals("receiver", messageHeaders.getReceiver());
		Assert.assertEquals("sender", messageHeaders.getSender());
		Assert.assertEquals(1, messageHeaders.getType());
		Assert.assertEquals(String.class, messageHeaders.getPayloadType());

	}
}
