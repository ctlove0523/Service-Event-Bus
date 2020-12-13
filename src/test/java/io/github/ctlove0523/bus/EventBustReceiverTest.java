package io.github.ctlove0523.bus;

import io.gtihub.ctlove0523.bus.EventBusReceiver;
import io.gtihub.ctlove0523.bus.LocalEventBus;
import io.gtihub.ctlove0523.bus.repository.InMemoryWaitSendAckMessageRepository;
import org.greenrobot.eventbus.Subscribe;

public class EventBustReceiverTest {
	@Subscribe
	public void process(String event) {
		System.out.println("receiver process " + event);
	}

	public static void main(String[] args) {
		LocalEventBus eventBus = LocalEventBus.localEventBus();
		eventBus.register(new EventBustReceiverTest());


		EventBusReceiver receiver = new EventBusReceiver(eventBus, 5432, new InMemoryWaitSendAckMessageRepository());
	}
}
