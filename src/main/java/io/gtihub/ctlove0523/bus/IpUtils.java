package io.gtihub.ctlove0523.bus;

import io.github.ctlove0523.commons.network.Ips;

public class IpUtils {

	public static String getCurrentListenIp() {
		return Ips.getIpByNetworkInterfaceName("eth0").getHostAddress();
	}
}
