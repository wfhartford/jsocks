package com.runjva.sourceforge.jsocks.protocol;

import java.util.concurrent.ExecutorService;

import com.runjva.sourceforge.jsocks.monitor.ProxyMonitor;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticator;

class ProxyServerParams {
  private final int idleTimeout;
  private final int acceptTimeout;
  private final SocksProxyBase proxy;
  private final ServerAuthenticator auth;
  private final ExecutorService executorService;
  private final ProxyMonitor monitor;

  ProxyServerParams(final int idleTimeout, final int acceptTimeout, final SocksProxyBase proxy,
      final ServerAuthenticator auth, final ExecutorService executorService, final ProxyMonitor monitor) {
    this.idleTimeout = idleTimeout;
    this.acceptTimeout = acceptTimeout;
    this.proxy = proxy;
    this.auth = auth;
    this.executorService = executorService;
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

  ExecutorService getExecutorService() {
    return executorService;
  }

  ProxyMonitor getMonitor() {
    return monitor;
  }
}
