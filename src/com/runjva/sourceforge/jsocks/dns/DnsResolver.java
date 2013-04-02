package com.runjva.sourceforge.jsocks.dns;

import java.net.InetAddress;
import java.net.UnknownHostException;

public interface DnsResolver {
	public InetAddress resolveByName(String host) throws UnknownHostException;
}
