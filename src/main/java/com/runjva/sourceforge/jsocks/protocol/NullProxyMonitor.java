package com.runjva.sourceforge.jsocks.protocol;

import java.net.Socket;
import java.net.SocketAddress;

public class NullProxyMonitor extends ProxyMonitor {
  public static final NullProxyMonitor INSTANCE = new NullProxyMonitor();
  @Override
  public Socket wrap(final StreamType type, final Socket socket) {
    return socket;
  }

  @Override
  protected void accountFor(final StreamType type, final StreamDirection direction,
      final SocketAddress address, final long bytes) {
  }
}
