package com.runjva.sourceforge.jsocks.main;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.dns.HashDnsResolver;
import com.runjva.sourceforge.jsocks.protocol.DnsProxyServer;
import com.runjva.sourceforge.jsocks.protocol.InetRange;
import com.runjva.sourceforge.jsocks.server.IdentAuthenticator;

public class DnsSocksMain {
	private static final Logger log = LoggerFactory
			.getLogger(DnsSocksMain.class);

	public static void main(String[] args) {
		// programmatic config
		int port = 1080;

		int iddleTimeout = 600000; // 10 minutes
		int acceptTimeout = 60000; // 1 minute
		int udpTimeout = 600000; // 10 minutes

		int socketBacklog = 5;
		String range = "localhost";

		// create minimal range based auth object
		final IdentAuthenticator auth = new IdentAuthenticator();
		InetRange irange = new InetRange();
		irange.add(range);
		auth.add(irange, null);

		// create dns proxy server
		
		Map<String, String> hosts = new HashMap<String, String>();
		hosts.put("www.yandex.ru", "213.180.193.3");
		hosts.put("yandex.ru", "213.180.193.3");
		hosts.put("www.ya.ru", "213.180.193.3");
		hosts.put("ya.ru", "213.180.193.3");

		DnsProxyServer proxyServer = new DnsProxyServer(auth, new HashDnsResolver(hosts));

		// config timeouts
		proxyServer.setIddleTimeout(iddleTimeout);
		proxyServer.setAcceptTimeout(acceptTimeout);
		proxyServer.setUDPTimeout(udpTimeout);

		InetAddress localIP = null;
		
		//start proxy server
		proxyServer.start(port, socketBacklog, localIP);
	}

}
