package com.runjva.sourceforge.jsocks.protocol;

import com.runjva.sourceforge.jsocks.server.ServerAuthenticator;

class ProxyServerParams {
  private final int idleTimeout;
  private final int acceptTimeout;
  private final SocksProxyBase proxy;
  private final ServerAuthenticator auth;

  ProxyServerParams(final int idleTimeout, final int acceptTimeout, final SocksProxyBase proxy,
      final ServerAuthenticator auth) {
    this.idleTimeout = idleTimeout;
    this.acceptTimeout = acceptTimeout;
    this.proxy = proxy;
    this.auth = auth;
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
}
