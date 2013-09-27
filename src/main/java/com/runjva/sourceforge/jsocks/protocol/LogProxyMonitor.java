package com.runjva.sourceforge.jsocks.protocol;

import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogProxyMonitor extends ProxyMonitor {
  private static final Logger log = LoggerFactory.getLogger(LogProxyMonitor.class);

  @Override
  protected void accountFor(final StreamType type, final StreamDirection direction,
      final SocketAddress address, final long bytes) {
    log.debug("{}({}) {}: {}", type, address, direction, bytes);
  }
}
