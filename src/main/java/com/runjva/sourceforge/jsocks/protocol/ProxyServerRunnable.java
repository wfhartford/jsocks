package com.runjva.sourceforge.jsocks.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.monitor.ProxyMonitor;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticator;

class ProxyServerRunnable implements Runnable {
  private static final Logger log = LoggerFactory.getLogger(ProxyServerRunnable.class);
  private static final int START_MODE = 0;
  private static final int ACCEPT_MODE = 1;
  private static final int PIPE_MODE = 2;
  private static final int ABORT_MODE = 3;
  private static final int BUF_SIZE = 8192;

  private ProxyMessage msg;

  private Socket sock, remote_sock;
  private UDPRelayServer relayServer;
  private InputStream in, remote_in;
  private OutputStream out, remote_out;
  private int mode;

  private Thread pipe_thread1, pipe_thread2;
  private ServerSocket ss;
  private long lastReadTime;

  private final int idleTimeout;
  private final int acceptTimeout;
  private final SocksProxyBase proxy;
  private ServerAuthenticator auth;
  private final ThreadFactory threadFactory;
  private final ProxyMonitor monitor;

  ProxyServerRunnable(final ProxyServerParams params, final Socket s) {
    this.idleTimeout = params.getIdleTimeout();
    this.acceptTimeout = params.getAcceptTimeout();
    this.proxy = params.getProxy();
    this.auth = params.getAuth();
    this.threadFactory = params.getThreadFactory();
    this.monitor = params.getMonitor();
    this.sock = s;
    this.mode = START_MODE;
  }

  // Runnable interface
// //////////////////
  @Override
  public void run() {
    switch (mode) {
      case START_MODE:
        try {
          startSession();
        }
        catch (final IOException ioe) {
          log.error("START_MODE exception.", ioe);
          handleException(ioe);
        }
        finally {
          abort();
          if (auth != null) {
            auth.endSession();
          }
          log.info("Main thread(client->remote)stopped.");
        }
        break;
      case ACCEPT_MODE:
        try {
          doAccept();
          mode = PIPE_MODE;
          pipe_thread1.interrupt(); // Tell other thread that connection
          // have
          // been accepted.
          pipe(remote_in, out);
        }
        catch (final IOException ioe) {
          log.error("ACCEPT_MODE exception.", ioe);
          handleException(ioe);
        }
        finally {
          abort();
          log.info("Accept thread(remote->client) stopped");
        }
        break;
      case PIPE_MODE:
        try {
          pipe(remote_in, out);
        }
        catch (final IOException ioe) {
          log.error("PIPE_MODE exception.", ioe);
        }
        finally {
          abort();
          log.info("Support thread(remote->client) stopped");
        }
        break;
      case ABORT_MODE:
        break;
      default:
        log.warn("Unexpected MODE " + mode);
    }
  }

  // Private methods
// ///////////////
  private void startSession() throws IOException {
    sock.setSoTimeout(idleTimeout);

    try {
      auth = auth.startSession(sock);
    }
    catch (final IOException ioe) {
      log.warn("Auth throwed exception:", ioe);
      auth = null;
      return;
    }

    if (auth == null) { // Authentication failed
      log.info("Authentication failed");
      return;
    }

    in = auth.getInputStream();
    out = auth.getOutputStream();

    msg = readMsg(in);
    handleRequest(msg);
  }

  private void handleRequest(final ProxyMessage msg) throws IOException {
    if (!auth.checkRequest(msg)) {
      throw new SocksException(SocksProxyBase.SOCKS_FAILURE);
    }

    if (msg.ip == null) {
      if (msg instanceof Socks5Message) {
        msg.ip = InetAddress.getByName(msg.host);
      }
      else {
        throw new SocksException(SocksProxyBase.SOCKS_FAILURE);
      }
    }
    log(msg);

    switch (msg.command) {
      case SocksProxyBase.SOCKS_CMD_CONNECT:
        onConnect(msg);
        break;
      case SocksProxyBase.SOCKS_CMD_BIND:
        onBind(msg);
        break;
      case SocksProxyBase.SOCKS_CMD_UDP_ASSOCIATE:
        onUDP(msg);
        break;
      default:
        throw new SocksException(SocksProxyBase.SOCKS_CMD_NOT_SUPPORTED);
    }
  }

  private void handleException(final IOException ioe) {
    // If we couldn't read the request, return;
    if (msg == null) {
      return;
    }
    // If have been aborted by other thread
    if (mode == ABORT_MODE) {
      return;
    }
    // If the request was successfully completed, but exception happened
    // later
    if (mode == PIPE_MODE) {
      return;
    }

    int error_code = SocksProxyBase.SOCKS_FAILURE;

    if (ioe instanceof SocksException) {
      error_code = ((SocksException) ioe).errCode;
    }
    else if (ioe instanceof NoRouteToHostException) {
      error_code = SocksProxyBase.SOCKS_HOST_UNREACHABLE;
    }
    else if (ioe instanceof ConnectException) {
      error_code = SocksProxyBase.SOCKS_CONNECTION_REFUSED;
    }
    else if (ioe instanceof InterruptedIOException) {
      error_code = SocksProxyBase.SOCKS_TTL_EXPIRE;
    }

    if ((error_code > SocksProxyBase.SOCKS_ADDR_NOT_SUPPORTED)
        || (error_code < 0)) {
      error_code = SocksProxyBase.SOCKS_FAILURE;
    }

    sendErrorMessage(error_code);
  }

  private void onConnect(final ProxyMessage msg) throws IOException {
    Socket s;

    if (proxy == null) {
      s = new Socket(msg.ip, msg.port);
    }
    else {
      s = new SocksSocket(proxy, msg.ip, msg.port);
    }

    log.info("Connected to " + s.getInetAddress() + ":" + s.getPort());

    ProxyMessage response = null;
    final InetAddress localAddress = s.getLocalAddress();
    final int localPort = s.getLocalPort();

    if (msg instanceof Socks5Message) {
      final int cmd = SocksProxyBase.SOCKS_SUCCESS;
      response = new Socks5Message(cmd, localAddress, localPort);
    }
    else {
      final int cmd = Socks4Message.REPLY_OK;
      response = new Socks4Message(cmd, localAddress, localPort);

    }
    response.write(out);
    startPipe(s);
  }

  private void onBind(final ProxyMessage msg) throws IOException {
    ProxyMessage response = null;

    if (proxy == null) {
      ss = new ServerSocket(0);
    }
    else {
      ss = new SocksServerSocket(proxy, msg.ip, msg.port);
    }

    ss.setSoTimeout(acceptTimeout);

    final InetAddress inetAddress = ss.getInetAddress();
    final int localPort = ss.getLocalPort();
    log.info("Trying accept on {}:{}", inetAddress, localPort);

    if (msg.version == 5) {
      final int cmd = SocksProxyBase.SOCKS_SUCCESS;
      response = new Socks5Message(cmd, inetAddress, localPort);
    }
    else {
      final int cmd = Socks4Message.REPLY_OK;
      response = new Socks4Message(cmd, inetAddress, localPort);
    }
    response.write(out);

    mode = ACCEPT_MODE;

    pipe_thread1 = Thread.currentThread();
    pipe_thread2 = threadFactory.newThread(this);
    pipe_thread2.start();

    // Make timeout infinit.
    sock.setSoTimeout(0);
    int eof = 0;

    try {
      while ((eof = in.read()) >= 0) {
        if (mode != ACCEPT_MODE) {
          if (mode != PIPE_MODE) {
            return;// Accept failed
          }

          remote_out.write(eof);
          break;
        }
      }
    }
    catch (final EOFException e) {
      log.debug("Connection closed while we were trying to accept", e);
      return;
    }
    catch (final InterruptedIOException e) {
      log.debug("Interrupted by unsucessful accept thread", e);
      if (mode != PIPE_MODE) {
        return;
      }
    }
    finally {
      // System.out.println("Finnaly!");
    }

    if (eof < 0) {
      return;
    }

    // Do not restore timeout, instead timeout is set on the
    // remote socket. It does not make any difference.

    pipe(in, remote_out);
  }

  private void onUDP(final ProxyMessage msg) throws IOException {
    if (msg.ip.getHostAddress().equals("0.0.0.0")) {
      msg.ip = sock.getInetAddress();
    }
    log.info("Creating UDP relay server for {}:{}", msg.ip, msg.port);

    relayServer = new UDPRelayServer(msg.ip, msg.port,
        Thread.currentThread(), sock, auth);

    ProxyMessage response;

    response = new Socks5Message(SocksProxyBase.SOCKS_SUCCESS,
        relayServer.relayIP, relayServer.relayPort);

    response.write(out);

    relayServer.start();

    // Make timeout infinit.
    sock.setSoTimeout(0);
    try {
      while (in.read() >= 0) {
      /* do nothing */
        ;
        // FIXME: Consider a slight delay here?
      }
    }
    catch (final EOFException eofe) {
    }
  }

// Private methods
// ////////////////

  private void doAccept() throws IOException {
    Socket s = null;
    final long startTime = System.currentTimeMillis();

    while (true) {
      s = ss.accept();
      if (s.getInetAddress().equals(msg.ip)) {
        // got the connection from the right host
        // Close listenning socket.
        ss.close();
        break;
      }
      else if (ss instanceof SocksServerSocket) {
        // We can't accept more then one connection
        s.close();
        ss.close();
        throw new SocksException(SocksProxyBase.SOCKS_FAILURE);
      }
      else {
        if (acceptTimeout != 0) { // If timeout is not infinit
          final long passed = System.currentTimeMillis() - startTime;
          final int newTimeout = acceptTimeout - (int) passed;

          if (newTimeout <= 0) {
            throw new InterruptedIOException("newTimeout <= 0");
          }
          ss.setSoTimeout(newTimeout);
        }
        s.close(); // Drop all connections from other hosts
      }
    }

    // Accepted connection
    remote_sock = monitor.monitor(ProxyMonitor.StreamEndpoint.REMOTE, s, auth.getAuthenticatedUser());
    remote_in = remote_sock.getInputStream();
    remote_out = remote_sock.getOutputStream();

    // Set timeout
    remote_sock.setSoTimeout(idleTimeout);

    final InetAddress inetAddress = remote_sock.getInetAddress();
    final int port = remote_sock.getPort();
    log.info("Accepted from {}:{}", remote_sock.getInetAddress(), port);

    ProxyMessage response;

    if (msg.version == 5) {
      final int cmd = SocksProxyBase.SOCKS_SUCCESS;
      response = new Socks5Message(cmd, inetAddress, port);
    }
    else {
      final int cmd = Socks4Message.REPLY_OK;
      response = new Socks4Message(cmd, inetAddress, port);
    }
    response.write(out);
  }

  private ProxyMessage readMsg(final InputStream in) throws IOException {
    PushbackInputStream push_in;
    if (in instanceof PushbackInputStream) {
      push_in = (PushbackInputStream) in;
    }
    else {
      push_in = new PushbackInputStream(in);
    }

    final int version = push_in.read();
    push_in.unread(version);

    ProxyMessage msg;

    if (version == 5) {
      msg = new Socks5Message(push_in, false);
    }
    else if (version == 4) {
      msg = new Socks4Message(push_in, false);
    }
    else {
      throw new SocksException(SocksProxyBase.SOCKS_FAILURE);
    }
    return msg;
  }

  private void startPipe(final Socket s) {
    mode = PIPE_MODE;
    try {
      remote_sock = monitor.monitor(ProxyMonitor.StreamEndpoint.REMOTE, s, auth.getAuthenticatedUser());;
      remote_in = remote_sock.getInputStream();
      remote_out = remote_sock.getOutputStream();
      pipe_thread1 = Thread.currentThread();
      pipe_thread2 = threadFactory.newThread(this);
      pipe_thread2.start();
      pipe(in, remote_out);
    }
    catch (final IOException ioe) {
    }
  }

  private void sendErrorMessage(final int error_code) {
    ProxyMessage err_msg;
    if (msg instanceof Socks4Message) {
      err_msg = new Socks4Message(Socks4Message.REPLY_REJECTED);
    }
    else {
      err_msg = new Socks5Message(error_code);
    }
    try {
      err_msg.write(out);
    }
    catch (final IOException ioe) {
    }
  }

  private synchronized void abort() {
    if (mode == ABORT_MODE) {
      return;
    }
    mode = ABORT_MODE;
    try {
      log.info("Aborting operation");
      if (null != in) {
        in.close();
      }
      if (null != out) {
        out.close();
      }
      if (null != remote_in) {
        remote_in.close();
      }
      if (null != remote_out) {
        remote_out.close();
      }
      if (remote_sock != null) {
        remote_sock.close();
      }
      if (sock != null) {
        sock.close();
      }
      if (relayServer != null) {
        relayServer.stop();
      }
      if (ss != null) {
        ss.close();
      }
      if (pipe_thread1 != null) {
        pipe_thread1.interrupt();
      }
      if (pipe_thread2 != null) {
        pipe_thread2.interrupt();
      }
    }
    catch (final IOException ioe) {
    }
  }

  static final void log(final ProxyMessage msg) {
    log.debug("Request version: {}, Command: ", msg.version,
        command2String(msg.command));

    final String user = msg.version == 4 ? ", User:" + msg.user : "";
    log.debug("IP:" + msg.ip + ", Port:" + msg.port + user);
  }

  private void pipe(final InputStream in, final OutputStream out)
      throws IOException {
    lastReadTime = System.currentTimeMillis();
    final byte[] buf = new byte[BUF_SIZE];
    int len = 0;
    while (len >= 0) {
      try {
        if (len != 0) {
          out.write(buf, 0, len);
          out.flush();
        }
        len = in.read(buf);
        lastReadTime = System.currentTimeMillis();
      }
      catch (final InterruptedIOException iioe) {
        if (idleTimeout == 0) {
          return;// Other thread interrupted us.
        }
        final long timeSinceRead = System.currentTimeMillis()
            - lastReadTime;

        if (timeSinceRead >= idleTimeout - 1000) {
          return;
        }
        len = 0;

      }
    }
  }

  static final String command_names[] = { "CONNECT", "BIND", "UDP_ASSOCIATE" };

  static final String command2String(int cmd) {
    if ((cmd > 0) && (cmd < 4)) {
      return command_names[cmd - 1];
    }
    else {
      return "Unknown Command " + cmd;
    }
  }
}
