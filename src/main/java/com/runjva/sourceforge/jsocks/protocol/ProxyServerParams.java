package com.runjva.sourceforge.jsocks.protocol;

import java.util.concurrent.ThreadFactory;

import com.runjva.sourceforge.jsocks.server.ServerAuthenticator;

class ProxyServerParams {
  private final int idleTimeout;
  private final int acceptTimeout;
  private final SocksProxyBase proxy;
  private final ServerAuthenticator auth;
  private final ThreadFactory threadFactory;
  private final ProxyMonitor monitor;

  ProxyServerParams(final int idleTimeout, final int acceptTimeout, final SocksProxyBase proxy,
      final ServerAuthenticator auth, final ThreadFactory threadFactory, final ProxyMonitor monitor) {
    this.idleTimeout = idleTimeout;
    this.acceptTimeout = acceptTimeout;
    this.proxy = proxy;
    this.auth = auth;
    this.threadFactory = threadFactory;
    this.monitor = monitor;
  }

  int getIdleTimeout() {
    return idleTimeout;
  }

  int getAcceptTimeout() {
    return acceptTimeout;
  }

  SocksProxyBase getProxy() {
    return proxy;
  }

  ServerAuthenticator getAuth() {
    return auth;
  }

  ThreadFactory getThreadFactory() {
    return threadFactory;
  }

  ProxyMonitor getMonitor() {
    return monitor;
  }
}
