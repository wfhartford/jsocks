package com.runjva.sourceforge.jsocks.monitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MonitorSocket extends Socket {
  private static final Logger log = LoggerFactory.getLogger(MonitorSocket.class);
  private final AtomicBoolean closed = new AtomicBoolean();
  private final long startTime = System.nanoTime();
  private final ProxyMonitor monitor;
  private final ProxyMonitor.StreamEndpoint type;
  private final String user;
  private final Socket socket;
  private final InputStream inputStream;
  private final OutputStream outputStream;
  private long runTime;

  public MonitorSocket(final ProxyMonitor monitor, final ProxyMonitor.StreamEndpoint type, final String user,
      final Socket socket) throws IOException {
    this.monitor = monitor;
    this.type = type;
    this.user = user;
    this.socket = socket;
    this.inputStream = new MonitorInputStream(this, socket.getInputStream());
    this.outputStream = new MonitorOutputStream(this, socket.getOutputStream());
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
    if (closed.compareAndSet(false, true)) {
      log.debug("Closing {}", this);
      runTime = System.nanoTime() - startTime;
      try {
        socket.close();
      }
      finally {
        try {
          inputStream.close();
        }
        finally {
          outputStream.close();
        }
      }
    }
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
  public InputStream getInputStream() {
    return inputStream;
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
  public OutputStream getOutputStream() {
    return outputStream;
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

  public void accountFor(final ProxyMonitor.StreamDirection direction, final long bytes) {
    final long time = 0 == runTime ? System.nanoTime() - startTime : runTime;
    monitor.accountFor(type, direction, getRemoteSocketAddress(), user, bytes, time);
  }
}
