package io.github.ctlove0523.bus;

import java.util.HashMap;
import java.util.Map;

import io.gtihub.ctlove0523.bus.Listener;
import org.junit.Test;

public class Demo {

	@Test
	public void test() {
		Map<String, String> test = new HashMap<>();
		test.put(AppListener.class.getName(), "APP");
		test.put(DeviceListener.class.getName(), "device");
		System.out.println(test.size());
		test.remove(new AppListener().getClass().getName());
		System.out.println(test.size());
	}
}

class AppListener implements Listener {

	@Override
	public void listen(Object event) {

	}
}

class DeviceListener implements Listener {

	@Override
	public void listen(Object event) {

	}
}