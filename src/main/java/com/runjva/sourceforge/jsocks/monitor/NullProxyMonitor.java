package com.runjva.sourceforge.jsocks.monitor;

import java.net.Socket;
import java.net.SocketAddress;

public class NullProxyMonitor extends ProxyMonitor {
  public static final NullProxyMonitor INSTANCE = new NullProxyMonitor();

  @Override
  public Socket monitor(final StreamEndpoint type, final Socket socket, final String user) {
    return socket;
  }

  @Override
  public void accountFor(final StreamEndpoint type, final StreamDirection direction,
      final SocketAddress address, final String user, final long bytes, final long runTime) {
  }
}
