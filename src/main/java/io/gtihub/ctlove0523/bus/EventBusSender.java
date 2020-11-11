package io.gtihub.ctlove0523.bus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import io.github.ctlove0523.commons.network.Ips;
import io.github.ctlove0523.discovery.api.Instance;
import io.github.ctlove0523.discovery.api.ServiceResolver;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EventBusSender {
	private static final Logger log = LoggerFactory.getLogger(EventBusSender.class);
	private final Vertx vertx = Vertx.vertx();
	private String serviceDomainName;
	private ServiceResolver serviceResolver;
	private int receiverPort;
	private Map<String, NetSocket> receivers = new HashMap<>();


	public void initReceivers() {
		List<Instance> instances = serviceResolver.resolve(serviceDomainName);
		instances.stream()
				.filter(new Predicate<Instance>() {
					@Override
					public boolean test(Instance instance) {
						return !instance.getAddress().getIpv4().equals(Ips.getIpByNetworkInterfaceName("eth0").getHostAddress());
					}
				}).forEach(new Consumer<Instance>() {
			@Override
			public void accept(Instance instance) {
				NetClient receiver = vertx.createNetClient();
				receiver.connect(receiverPort, instance.getAddress().getIpv4(), new Handler<AsyncResult<NetSocket>>() {
					@Override
					public void handle(AsyncResult<NetSocket> event) {
						if (event.succeeded()) {
							log.info("connect to receiver {} success", instance.getAddress().getIpv4());
							receivers.put(instance.getAddress().getIpv4(), event.result());
							event.result().handler(new Handler<Buffer>() {
								@Override
								public void handle(Buffer event) {

								}
							});
						}
						else {
							log.warn("connect to receiver {} failed", instance.getAddress().getIpv4());
							receivers.remove(instance.getAddress().getIpv4());
						}
					}
				});
			}
		});
	}
}
