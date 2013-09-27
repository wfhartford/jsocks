package com.runjva.sourceforge.jsocks.protocol;

import org.junit.Test;

import com.runjva.sourceforge.jsocks.dns.DnsResolverFactory;
import com.runjva.sourceforge.jsocks.server.IdentAuthenticator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DnsProxyServerTest {

  private static final long WHAIT_PROXY_START_TIMEOUT = 500;
  private static final int DEFAUT_PROXY_PORT = 1080;

  private DnsProxyServer getDnsProxyServerInstance() {
    int iddleTimeout = 600000; // 10 minutes
    int acceptTimeout = 60000; // 1 minute
    int udpTimeout = 600000; // 10 minutes

    String range = "localhost";

    // create minimal range based auth object
    final IdentAuthenticator auth = new IdentAuthenticator();
    InetRange irange = new InetRange();
    irange.add(range);
    auth.add(irange, null);

    // create dns proxy server
    DnsProxyServer dnsProxyServer = new DnsProxyServer(auth, DnsResolverFactory.getDefaultDnsResolverInstance());

    // config timeouts
    dnsProxyServer.setIddleTimeout(iddleTimeout);
    dnsProxyServer.setAcceptTimeout(acceptTimeout);
    dnsProxyServer.setUDPTimeout(udpTimeout);

    return dnsProxyServer;
  }

  @Test
  public void shouldInitialyBeStoped() {
    DnsProxyServer proxyServer = getDnsProxyServerInstance();
    assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
  }

  @Test
  public void shouldBeStartedAfterStart() {
    DnsProxyServer proxyServer = getDnsProxyServerInstance();
    assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
    startProxy(proxyServer, DEFAUT_PROXY_PORT);

    // sleep for a while to let proxy to start
    sleep(WHAIT_PROXY_START_TIMEOUT);

    boolean isStarted = ProxyStatus.STARTED.equals(proxyServer.getProxyStatus());
    //stop proxy to free port before assertion check
    proxyServer.stop();

    //check that proxy was in started state
    assertTrue(isStarted);
  }

  @Test
  public void shouldBeStopedAfterStop() {
    DnsProxyServer proxyServer = getDnsProxyServerInstance();
    startProxy(proxyServer, DEFAUT_PROXY_PORT);
    sleep(WHAIT_PROXY_START_TIMEOUT);

    //proxy should be started
    boolean isStarted = ProxyStatus.STARTED.equals(proxyServer.getProxyStatus());
    //stop proxy to free port before assertion check
    proxyServer.stop();

    //check that proxy was in started state
    assertTrue(isStarted);

    assertEquals(ProxyStatus.STOPED, proxyServer.getProxyStatus());
  }

  @Test
  public void shouldBeErrorOnException() {
    //start another proxy to bind port
    DnsProxyServer anotherProxyServer = getDnsProxyServerInstance();
    startProxy(anotherProxyServer, DEFAUT_PROXY_PORT);
    sleep(WHAIT_PROXY_START_TIMEOUT);


    DnsProxyServer proxyServer = getDnsProxyServerInstance();
    startProxy(proxyServer, DEFAUT_PROXY_PORT);
    sleep(WHAIT_PROXY_START_TIMEOUT);

    //should be exception because port already taken
    assertEquals(ProxyStatus.ERROR, proxyServer.getProxyStatus());
  }

  private void sleep(long timeToSleep) {
    try {
      Thread.sleep(timeToSleep);
    }
    catch (InterruptedException e) {
    }
  }

  private void startProxy(final DnsProxyServer proxyServer, final int port) {
    new Thread(new Runnable() {

      @Override
      public void run() {
        proxyServer.start(port);
      }
    }).start();
  }
}
