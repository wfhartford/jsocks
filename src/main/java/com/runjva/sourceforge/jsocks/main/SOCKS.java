package com.runjva.sourceforge.jsocks.main;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Properties;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.protocol.InetRange;
import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.protocol.SocksProxyBase;
import com.runjva.sourceforge.jsocks.server.IdentAuthenticator;

public class SOCKS {

	private static final String DEFAULT_CONFIG_FILE = "src/main/resources/socks.properties";
	private static final int DEFAULT_LISTENING_PORT = 1080;
	private static final Logger log = LoggerFactory.getLogger(SOCKS.class);

	private static void usage() {
		log.info(
				String.format("Usage: java SOCKS inifile%nIf none inifile is given, uses %s.%n",
				DEFAULT_CONFIG_FILE));
	}

	static public void main(String[] args) {

		String configFileName = DEFAULT_CONFIG_FILE;
		int port = DEFAULT_LISTENING_PORT;

		final IdentAuthenticator auth = new IdentAuthenticator();

		InetAddress localIP = null;

		if (args.length != 0) {
			configFileName = args[0];
		}

		inform("Loading properties");
		//load from configured path
		log.info(String.format("Try load properties from path %s.", configFileName));
		Properties properties = loadProperties(configFileName);
		if (properties == null) {
			log.warn(String.format("Loading of properties from %s failed.", configFileName));

			//load from default path
			log.info(String.format("Try load properties from default path %s.", DEFAULT_CONFIG_FILE));

			properties = loadProperties(DEFAULT_CONFIG_FILE);
			if(properties == null){
				log.warn(String.format("Loading of properties from default path %s failed.", DEFAULT_CONFIG_FILE));
				usage();
				return;
			}			
		}
		
		

		// check auth info provided and wellformed
		if (!addAuth(auth, properties)) {
			log.error("Error auth info(range, user) in config file %s.",
					configFileName);
			usage();
			return;
		}

		final String port_s = (String) properties.get("port");
		if (port_s != null) {
			try {
				port = Integer.parseInt(port_s);
			} catch (final NumberFormatException nfe) {
				log.error("Can't parse port: " + port_s);
				return;
			}
		}

		String host = (String) properties.get("host");
		if (host != null) {
			try {
				localIP = InetAddress.getByName(host);
			} catch (final UnknownHostException uhe) {
				log.error("Can't resolve local ip: " + host);
				return;
			}
		}

		inform("Using Ident Authentication scheme: " + auth);
		// create proxy
		final ProxyServer server = new ProxyServer(auth);

		// config
		serverInit(server, properties);
		proxyInit(server, properties);

		// start
		server.start(port, 5, localIP);
	}

	private static Properties loadProperties(String file_name) {

		final Properties pr = new Properties();

		try {
			final InputStream fin = new FileInputStream(file_name);
			pr.load(fin);
			fin.close();
		} catch (final IOException ioe) {
			return null;
		}
		return pr;
	}

	private static boolean addAuth(IdentAuthenticator ident, Properties pr) {

		InetRange irange;

		final String range = (String) pr.get("range");
		if (range == null) {
			return false;
		}
		irange = parseInetRange(range);

		final String users = (String) pr.get("users");

		if (users == null) {
			ident.add(irange, null);
			return true;
		}

		final Hashtable<String, String> uhash = new Hashtable<String, String>();

		final StringTokenizer st = new StringTokenizer(users, ";");
		while (st.hasMoreTokens()) {
			uhash.put(st.nextToken(), "");
		}

		ident.add(irange, uhash);
		return true;
	}

	/**
	 * Does server initialisation.
	 */
	private static void serverInit(ProxyServer proxyServer, Properties props) {
		int val;
		val = readInt(props, "iddleTimeout");
		if (val >= 0) {
			proxyServer.setIddleTimeout(val);
			inform("Setting iddle timeout to " + val + " ms.");
		}
		val = readInt(props, "acceptTimeout");
		if (val >= 0) {
			proxyServer.setAcceptTimeout(val);
			inform("Setting accept timeout to " + val + " ms.");
		}
		val = readInt(props, "udpTimeout");
		if (val >= 0) {
			proxyServer.setUDPTimeout(val);
			inform("Setting udp timeout to " + val + " ms.");
		}

		val = readInt(props, "datagramSize");
		if (val >= 0) {
			proxyServer.setDatagramSize(val);
			inform("Setting datagram size to " + val + " bytes.");
		}
	}

	/**
	 * Initialises proxy, if any specified.
	 */
	private static void proxyInit(ProxyServer proxyServer, Properties props) {
		String proxy_list;
		SocksProxyBase proxy = null;
		StringTokenizer st;

		proxy_list = (String) props.get("proxy");
		if (proxy_list == null) {
			return;
		}

		st = new StringTokenizer(proxy_list, ";");
		while (st.hasMoreTokens()) {
			final String proxy_entry = st.nextToken();

			final SocksProxyBase p = SocksProxyBase.parseProxy(proxy_entry);

			if (p == null) {
				exit("Can't parse proxy entry:" + proxy_entry);
			}

			inform("Adding Proxy:" + p);

			if (proxy != null) {
				p.setChainProxy(proxy);
			}

			proxy = p;

		}
		if (proxy == null) {
			return; // Empty list
		}

		final String direct_hosts = (String) props.get("directHosts");
		if (direct_hosts != null) {
			final InetRange ir = parseInetRange(direct_hosts);
			inform("Setting direct hosts:" + ir);
			proxy.setDirect(ir);
		}

		proxyServer.setProxy(proxy);
	}

	/**
	 * Inits range from the string of semicolon separated ranges.
	 */
	private static InetRange parseInetRange(String source) {
		final InetRange irange = new InetRange();

		final StringTokenizer st = new StringTokenizer(source, ";");
		while (st.hasMoreTokens()) {
			irange.add(st.nextToken());
		}

		return irange;
	}

	/**
	 * Integer representaion of the property named name, or -1 if one is not
	 * found.
	 */
	private static int readInt(Properties props, String name) {
		int result = -1;
		final String val = (String) props.get(name);
		if (val == null) {
			return -1;
		}
		final StringTokenizer st = new StringTokenizer(val);
		if (!st.hasMoreElements()) {
			return -1;
		}
		try {
			result = Integer.parseInt(st.nextToken());
		} catch (final NumberFormatException nfe) {
			inform("Bad value for " + name + ":" + val);
		}
		return result;
	}

	// Display functions
	private static void inform(String s) {
		log.info(s);
	}

	private static void exit(String msg) {
		log.error("Error:" + msg);
		log.error("Aborting operation");
		System.exit(0);
	}
}
