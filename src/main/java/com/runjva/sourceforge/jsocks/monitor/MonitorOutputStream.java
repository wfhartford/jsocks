package com.runjva.sourceforge.jsocks.monitor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingOutputStream;

public final class MonitorOutputStream extends FilterOutputStream {
  private static final Logger log = LoggerFactory.getLogger(MonitorOutputStream.class);
  private final AtomicBoolean closed = new AtomicBoolean();
  private final MonitorSocket socket;

  public MonitorOutputStream(final MonitorSocket socket, final OutputStream stream) {
    super(new CountingOutputStream(stream));
    this.socket = socket;
  }

  @Override
  public void write(final int b) throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    out.write(b);
  }

  @Override
  public void write(final byte[] b) throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    out.write(b);
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (closed.get()) {
      throw new IllegalStateException("Already closed");
    }
    out.write(b, off, len);
  }

  @Override
  public void close() throws IOException {
    if (closed.compareAndSet(false, true)) {
      log.debug("Closing {}", this);
      try {
        socket.accountFor(ProxyMonitor.StreamDirection.OUTPUT, ((CountingOutputStream) out).getCount());
      }
      finally {
        super.close();
      }
    }
  }
}
