package com.runjva.sourceforge.jsocks.monitor;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

public final class MonitorInputStream extends FilterInputStream {
  private static final Logger log = LoggerFactory.getLogger(MonitorInputStream.class);
  private final AtomicBoolean closed = new AtomicBoolean();
  private final MonitorSocket socket;

  public MonitorInputStream(final MonitorSocket socket, final InputStream stream) {
    super(new CountingInputStream(stream));
    this.socket = socket;
  }

  @Override
  public int read() throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    return in.read();
  }

  @Override
  public int read(final byte[] b) throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    return in.read(b);
  }

  @Override
  public int read(final byte[] b, final int off, final int len) throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    return in.read(b, off, len);
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      log.debug("Closing {}", this);
      try {
        socket.accountFor(ProxyMonitor.StreamDirection.INPUT, ((CountingInputStream) in).getCount());
      }
      finally {
        super.close();
      }
    }
  }
}
