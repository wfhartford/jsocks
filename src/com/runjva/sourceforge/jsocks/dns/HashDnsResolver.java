package com.runjva.sourceforge.jsocks.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HashDnsResolver implements DnsResolver {
	final private static Logger log = LoggerFactory
			.getLogger(HashDnsResolver.class);
	private Map<String, String> hosts = new HashMap<String, String>();

	/**
	 * Create instance of hashbased dns recolver
	 * 
	 * @param hosts
	 *            Map with host value as key and ip address as value
	 */
	public HashDnsResolver(Map<String, String> hosts) {
		if (hosts != null) {
			this.hosts = hosts;
		}
	}

	@Override
	public InetAddress resolveByName(String host) throws UnknownHostException {
		InetAddress address = null;
		String addressToResolve = host;
		if (hosts.containsKey(host)) {
			addressToResolve = hosts.get(host);
			log.info("resolved from hash ip=" + addressToResolve + " for host=" + host);
		} else {
			log.info("not resolved from hash host=" + host);
		}
		address = InetAddress.getByName(addressToResolve);

		return address;
	}
}
