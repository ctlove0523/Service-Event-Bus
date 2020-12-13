package io.github.ctlove0523.bus;

import java.util.concurrent.TimeUnit;

import io.gtihub.ctlove0523.bus.EventBusSender;
import io.gtihub.ctlove0523.bus.LocalEventBus;
import io.gtihub.ctlove0523.bus.repository.InMemoryWaitAckMessageRepository;
import org.greenrobot.eventbus.Subscribe;

public class EventBusSenderTest {
	@Subscribe
	public void process(String event) {
		System.out.println("process " + event);
	}

	public static void main(String[] args) throws Exception {
		LocalEventBus eventBus = LocalEventBus.localEventBus();
		eventBus.register(new EventBusSenderTest());
		EventBusSender sender = new EventBusSender("TEST", 5432, eventBus, new InMemoryWaitAckMessageRepository());

		TimeUnit.SECONDS.sleep(1);
		sender.post("hello receiver", 10 * 1000);
	}
}
