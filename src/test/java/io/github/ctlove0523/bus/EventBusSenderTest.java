package io.github.ctlove0523.bus;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.github.ctlove0523.discovery.api.Instance;
import io.github.ctlove0523.discovery.api.InstanceAddress;
import io.github.ctlove0523.discovery.api.ServiceResolver;
import io.gtihub.ctlove0523.bus.EventBusSender;
import io.gtihub.ctlove0523.bus.LocalEventBus;
import org.greenrobot.eventbus.Subscribe;

public class EventBusSenderTest {
	@Subscribe
	public void process(String event) {
		System.out.println("process " + event);
	}
	public static void main(String[] args) throws Exception {
		LocalEventBus eventBus = LocalEventBus.localEventBus();
		eventBus.register(new EventBusSenderTest());
		EventBusSender sender = new EventBusSender("TEST", new ServiceResolver() {
			@Override
			public List<Instance> resolve(String s) {
				InstanceAddress address = new InstanceAddress("127.0.0.1");
				Instance instance = new Instance(address);
				return Collections.singletonList(instance);
			}

			@Override
			public int getOrder() {
				return 0;
			}
		}, 5432, eventBus);

		TimeUnit.SECONDS.sleep(1);
		sender.post("hello receiver");
	}
}
