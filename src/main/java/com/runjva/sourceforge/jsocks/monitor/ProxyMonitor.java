package com.runjva.sourceforge.jsocks.monitor;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProxyMonitor {
  private static final Logger log = LoggerFactory.getLogger(ProxyMonitor.class);

  public enum StreamEndpoint {CLIENT, REMOTE}

  public enum StreamDirection {INPUT, OUTPUT}

  public enum StreamCounter {CLIENT_IN, CLIENT_OUT, REMOTE_IN, REMOTE_OUT}

  public Socket monitor(final StreamEndpoint type, final Socket socket, final String user) throws IOException {
    log.debug("Wrapping {} Socket for {}", type, user);
    return new MonitorSocket(this, type, user, socket);
  }

  public abstract void accountFor(final StreamEndpoint type, final StreamDirection direction,
      final SocketAddress address, final String user, final long bytes, final long runTimeNanos);
}
