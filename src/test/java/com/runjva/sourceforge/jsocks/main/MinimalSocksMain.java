package com.runjva.sourceforge.jsocks.main;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.protocol.InetRange;
import com.runjva.sourceforge.jsocks.protocol.ProxyServer;
import com.runjva.sourceforge.jsocks.server.IdentAuthenticator;

/**
 * Minimal configured SocksProxy for manual testing
 */
public class MinimalSocksMain {
  private static final Logger log = LoggerFactory
      .getLogger(MinimalSocksMain.class);

  public static void main(String[] args) {
    // programmatic config
    int port = 1080;

    int iddleTimeout = 600000; // 10 minutes
    int acceptTimeout = 60000; // 1 minute
    int udpTimeout = 600000; // 10 minutes

    String range = "localhost";

    // create minimal range based auth object
    final IdentAuthenticator auth = new IdentAuthenticator();
    InetRange irange = new InetRange();
    irange.add(range);
    auth.add(irange, null);

    // create proxy server
    ProxyServer proxyServer = new ProxyServer(auth);

    // config timeouts
    proxyServer.setIddleTimeout(iddleTimeout);
    proxyServer.setAcceptTimeout(acceptTimeout);
    proxyServer.setUDPTimeout(udpTimeout);

    //start proxy server
    proxyServer.start(port);
  }

}
