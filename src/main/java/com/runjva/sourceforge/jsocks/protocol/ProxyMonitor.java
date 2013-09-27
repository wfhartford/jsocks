package com.runjva.sourceforge.jsocks.protocol;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;
import com.google.common.io.CountingOutputStream;

public abstract class ProxyMonitor {
  private static final Logger log = LoggerFactory.getLogger(ProxyMonitor.class);

  private final class MonitorSocket extends Socket {
    private final StreamType type;
    private final Socket socket;

    private MonitorSocket(final StreamType type, final Socket socket) {
      this.type = type;
      this.socket = socket;
    }

    @Override
    public void connect(final SocketAddress endpoint) throws IOException {
      socket.connect(endpoint);
    }

    @Override
    public boolean isBound() {
      return socket.isBound();
    }

    @Override
    public int getReceiveBufferSize() throws SocketException {
      return socket.getReceiveBufferSize();
    }

    @Override
    public boolean isInputShutdown() {
      return socket.isInputShutdown();
    }

    @Override
    public int getSoTimeout() throws SocketException {
      return socket.getSoTimeout();
    }

    @Override
    public void sendUrgentData(final int data) throws IOException {
      socket.sendUrgentData(data);
    }

    @Override
    public void close() throws IOException {
      socket.close();
    }

    @Override
    public boolean isConnected() {
      return socket.isConnected();
    }

    @Override
    public void shutdownInput() throws IOException {
      socket.shutdownInput();
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return wrap(type, socket.getInputStream(), getRemoteSocketAddress());
    }

    @Override
    public void setReceiveBufferSize(final int size) throws SocketException {
      socket.setReceiveBufferSize(size);
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
      return socket.getRemoteSocketAddress();
    }

    @Override
    public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
      socket.connect(endpoint, timeout);
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
      return socket.getReuseAddress();
    }

    @Override
    public void setReuseAddress(final boolean on) throws SocketException {
      socket.setReuseAddress(on);
    }

    @Override
    public void shutdownOutput() throws IOException {
      socket.shutdownOutput();
    }

    @Override
    public void setOOBInline(final boolean on) throws SocketException {
      socket.setOOBInline(on);
    }

    @Override
    public boolean isClosed() {
      return socket.isClosed();
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
      return wrap(type, socket.getOutputStream(), getRemoteSocketAddress());
    }

    @Override
    public int getPort() {
      return socket.getPort();
    }

    @Override
    public int getTrafficClass() throws SocketException {
      return socket.getTrafficClass();
    }

    @Override
    public boolean isOutputShutdown() {
      return socket.isOutputShutdown();
    }

    @Override
    public void bind(final SocketAddress bindpoint) throws IOException {
      socket.bind(bindpoint);
    }

    @Override
    public int getLocalPort() {
      return socket.getLocalPort();
    }

    @Override
    public void setTcpNoDelay(final boolean on) throws SocketException {
      socket.setTcpNoDelay(on);
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
      return socket.getKeepAlive();
    }

    @Override
    public void setKeepAlive(final boolean on) throws SocketException {
      socket.setKeepAlive(on);
    }

    @Override
    public int getSoLinger() throws SocketException {
      return socket.getSoLinger();
    }

    @Override
    public InetAddress getInetAddress() {
      return socket.getInetAddress();
    }

    @Override
    public boolean getOOBInline() throws SocketException {
      return socket.getOOBInline();
    }

    @Override
    public void setSendBufferSize(final int size) throws SocketException {
      socket.setSendBufferSize(size);
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
      return socket.getLocalSocketAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
      return socket.getLocalAddress();
    }

    @Override
    public void setPerformancePreferences(final int connectionTime, final int latency, final int bandwidth) {
      socket.setPerformancePreferences(connectionTime, latency, bandwidth);
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
      return socket.getTcpNoDelay();
    }

    @Override
    public SocketChannel getChannel() {
      throw new UnsupportedOperationException();
    }

    @Override
    public void setSoTimeout(final int timeout) throws SocketException {
      socket.setSoTimeout(timeout);
    }

    @Override
    public void setSoLinger(final boolean on, final int linger) throws SocketException {
      socket.setSoLinger(on, linger);
    }

    @Override
    public int getSendBufferSize() throws SocketException {
      return socket.getSendBufferSize();
    }

    @Override
    public void setTrafficClass(final int tc) throws SocketException {
      socket.setTrafficClass(tc);
    }
  }

  private final class MonitorInputStream extends FilterInputStream {
    private final StreamType type;
    private final SocketAddress address;

    MonitorInputStream(final StreamType type, final SocketAddress address, final InputStream in) {
      super(new CountingInputStream(in));
      this.type = type;
      this.address = address;
    }

    @Override
    public int read() throws IOException {
      return in.read();
    }

    @Override
    public int read(final byte[] b) throws IOException {
      return in.read(b);
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      return in.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
      try {
        accountFor(type, StreamDirection.INPUT, address, ((CountingInputStream) in).getCount());
      }
      finally {
        super.close();
      }
    }
  }

  private final class MonitorOutputStream extends FilterOutputStream {
    private final StreamType type;
    private final SocketAddress address;

    MonitorOutputStream(final StreamType type, final SocketAddress address, final OutputStream out) {
      super(new CountingOutputStream(out));
      this.type = type;
      this.address = address;
    }

    @Override
    public void write(final int b) throws IOException {
      out.write(b);
    }

    @Override
    public void write(final byte[] b) throws IOException {
      out.write(b);
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
      out.write(b, off, len);
    }

    @Override
    public void close() throws IOException {
      try {
        accountFor(type, StreamDirection.OUTPUT, address, ((CountingOutputStream) out).getCount());
      }
      finally {
        super.close();
      }
    }
  }

  public enum StreamType {CLIENT, REMOTE}

  public enum StreamDirection {INPUT, OUTPUT}

  public Socket wrap(final StreamType type, final Socket socket) {
    log.debug("Wrapping {} Socket", type);
    return new MonitorSocket(type, socket);
  }

  private InputStream wrap(final StreamType type, final InputStream in, final SocketAddress address) {
    log.debug("Wrapping {} InputStream", type);
    return new MonitorInputStream(type, address, in);
  }

  private OutputStream wrap(final StreamType type, final OutputStream out, final SocketAddress address) {
    log.debug("Wrapping {} OutputStream", type);
    return new MonitorOutputStream(type, address, out);
  }

  protected abstract void accountFor(final StreamType type, final StreamDirection direction,
      final SocketAddress address, final long bytes);
}
