package com.runjva.sourceforge.jsocks.monitor;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogProxyMonitor extends ProxyMonitor {
  private static final Logger log = LoggerFactory.getLogger(LogProxyMonitor.class);

  public static final LogProxyMonitor INSTANCE = new LogProxyMonitor();

  @Override
  public void accountFor(final StreamEndpoint type, final StreamDirection direction,
      final SocketAddress address, final String user, final long bytes, final long runTime) {
    final double bps = bytes / ((double) runTime / (double) TimeUnit.SECONDS.toNanos(1));
    log.debug("{}: {}({}) {}: {}/{}ns = {} B/s", user, type, address, direction, bytes, runTime, bps);
  }

  public static ProxyMonitor get() {
    return log.isDebugEnabled() ? INSTANCE : NullProxyMonitor.INSTANCE;
  }
}
