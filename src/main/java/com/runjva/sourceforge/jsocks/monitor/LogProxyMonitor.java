package com.runjva.sourceforge.jsocks.monitor;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogProxyMonitor extends ProxyMonitor {
  private static final Logger log = LoggerFactory.getLogger(LogProxyMonitor.class);

  public static final LogProxyMonitor INSTANCE = new LogProxyMonitor();

  @Override
  public void accountFor(final StreamEndpoint type, final StreamDirection direction,
      final SocketAddress address, final String user, final long bytes) {
    log.debug("{}: {}({}) {}: {}", user, type, address, direction, bytes);
  }
}
