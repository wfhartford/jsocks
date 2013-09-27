package com.runjva.sourceforge.jsocks.protocol;

import org.junit.Test;

import com.runjva.sourceforge.jsocks.server.IdentAuthenticator;

import static org.junit.Assert.assertEquals;

public class ProxyServerTest {

  private static final long WHAIT_PROXY_START_TIMEOUT = 500;
  private static final int DEFAUT_PROXY_PORT = 10808;

  private ProxyServer getProxyServerInstance() {
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
    proxyServer.setIdleTimeout(iddleTimeout);
    proxyServer.setAcceptTimeout(acceptTimeout);
    proxyServer.setUDPTimeout(udpTimeout);

    return proxyServer;
  }

  @Test
  public void shouldInitialyBeStoped() {
    ProxyServer proxyServer = getProxyServerInstance();
    assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
  }

  @Test
  public void shouldBeStartedAfterStart() {
    ProxyServer proxyServer = getProxyServerInstance();
    try {
      assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
      startProxy(proxyServer, DEFAUT_PROXY_PORT);

      // sleep for a while to let proxy to start
      sleep(WHAIT_PROXY_START_TIMEOUT);
      assertEquals(ProxyStatus.STARTED, proxyServer.getProxyStatus());
    }
    finally {
      //stop proxy to free port before assertion check
      proxyServer.stop();
    }
  }

  @Test
  public void shouldBeStopedAfterStop() {
    ProxyServer proxyServer = getProxyServerInstance();
    try {
      assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
      startProxy(proxyServer, DEFAUT_PROXY_PORT);

      // sleep for a while to let proxy to start
      sleep(WHAIT_PROXY_START_TIMEOUT);
      assertEquals(ProxyStatus.STARTED, proxyServer.getProxyStatus());
    }
    finally {
      //stop proxy to free port before assertion check
      proxyServer.stop();
      assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
    }
  }

  @Test
  public void shouldBeErrorOnException() {
    ProxyServer anotherProxyServer = getProxyServerInstance();
    try {
      ProxyServer proxyServer = getProxyServerInstance();
      try {
        //start another proxy to bind port
        startProxy(anotherProxyServer, DEFAUT_PROXY_PORT);
        sleep(WHAIT_PROXY_START_TIMEOUT);
        assertEquals(ProxyStatus.STARTED, anotherProxyServer.getProxyStatus());

        startProxy(proxyServer, DEFAUT_PROXY_PORT);
        sleep(WHAIT_PROXY_START_TIMEOUT);

        //should be exception because port already taken
        assertEquals(ProxyStatus.ERROR, proxyServer.getProxyStatus());
      }
      finally {
        proxyServer.stop();
      }
    }
    finally {
      anotherProxyServer.stop();
    }
  }

  private void sleep(long timeToSleep) {
    try {
      Thread.sleep(timeToSleep);
    }
    catch (InterruptedException e) {
    }
  }

  private void startProxy(final ProxyServer proxyServer, final int port) {
    new Thread(new Runnable() {

      @Override
      public void run() {
        proxyServer.start(port);
      }
    }).start();
  }
}
