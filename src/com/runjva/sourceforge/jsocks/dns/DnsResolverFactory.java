package com.runjva.sourceforge.jsocks.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnsResolverFactory {
	final private static Logger log = LoggerFactory
			.getLogger(DnsResolverFactory.class);

	private DnsResolverFactory() {
	}

	public static DnsResolver getDefaultDnsResolverInstance() {
		return new DefaultDnsResolver();
	}
}

class DefaultDnsResolver implements DnsResolver {
	private static final Logger log = LoggerFactory
			.getLogger(DnsResolverFactory.class);

	@Override
	public InetAddress resolveByName(String host) throws UnknownHostException {
		InetAddress inetAddress = InetAddress.getByName(host);
		log.info("resolve by default dns resolver " + inetAddress);
		return inetAddress;
	}
}