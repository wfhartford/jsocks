package com.runjva.sourceforge.jsocks.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import sun.net.util.IPAddressUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of DnsResolver with custom configured mapping of ip addresses for domains.
 * It gives ability to config custom mapping like in etc/hosts for each proxy instance.
 * Resolver first tries take value from configured mapping and uses standard dns lookup as fallback.
 */
@SuppressWarnings("restriction")
public class HashDnsResolver implements DnsResolver {
	final private static Logger log = LoggerFactory
			.getLogger(HashDnsResolver.class);
	private final Map<String, String> hosts;

	/**
	 * Create instance of hashbased dns recolver
	 * 
	 * @param hosts
	 *            Map with host value as key and ip address as value
	 */
	public HashDnsResolver(Map<String, String> hosts) {
		this.hosts = hosts;
	}

	@Override
	public InetAddress resolveByName(String host) throws UnknownHostException {
		InetAddress address = null;
		
		if (hosts.containsKey(host)) {
			String ip = hosts.get(host);
			byte[] ipAsBytes = IPAddressUtil.textToNumericFormatV4(ip);
			if(ipAsBytes == null){
				log.warn(String.format("Can't convert string ip=%s to byte array", ip));
				throw new UnknownHostException("Wrong ip format " + ip);
			}
			
			//create address without lookup
			address = InetAddress.getByAddress(host, ipAsBytes);				
			log.info("resolved from hash ip=" + ip + " for host="
					+ host);					
		} else {
			//standard dns lookup as fallback
			address = InetAddress.getByName(host);
			log.info("not resolved from hash host=" + host);
		}
		
		return address;
	}
}
