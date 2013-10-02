package com.runjva.sourceforge.jsocks.protocol;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.monitor.LogProxyMonitor;
import com.runjva.sourceforge.jsocks.monitor.ProxyMonitor;
import com.runjva.sourceforge.jsocks.server.ServerAuthenticator;

/**
 * SOCKS4 and SOCKS5 proxy, handles both protocols simultaniously. Implements
 * all SOCKS commands, including UDP relaying.
 * <p/>
 * In order to use it you will need to implement ServerAuthenticator interface.
 * There is an implementation of this interface which does no authentication
 * ServerAuthenticatorNone, but it is very dangerous to use, as it will give
 * access to your local network to anybody in the world. One should never use
 * this authentication scheme unless one have pretty good reason to do so. There
 * is a couple of other authentication schemes in socks.server package.
 *
 * @see ServerAuthenticator
 */
public class ProxyServer {

  private static final Logger log = LoggerFactory.getLogger(ProxyServer.class);

  private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
      new Thread.UncaughtExceptionHandler() {

        @Override
        public void uncaughtException(final Thread t, final Throwable e) {
          log.warn("Thread {} threw uncaught exception", t, e);
        }
      };

  private static final ThreadFactory DEFAULT_THREAD_FACTORY = new ThreadFactory() {
    private final AtomicLong COUNTER = new AtomicLong();

    @Override
    public Thread newThread(final Runnable runnable) {
      final Thread thread = new Thread(runnable);
      thread.setName("ProxyServer-thread-" + COUNTER.incrementAndGet());
      thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
      return thread;
    }
  };

  private ServerSocket ss;
  private final ServerAuthenticator auth;
  private final ProxyMonitor monitor;


  private final ExecutorService executorService;
  private SocksProxyBase proxy;
  private final Object statusMutex = new Object();
  private ProxyStatus proxyStatus = ProxyStatus.STOPED;
  private int idleTimeout = 180000; // 3 minutes
  private int acceptTimeout = 180000; // 3 minutes

  /**
   * Creates a proxy server with the given Authentication scheme, using the default thread factory and no monitoring.
   *
   * @param auth
   *     Authentication scheme to be used.
   */
  public ProxyServer(final ServerAuthenticator auth) {
    this(auth, DEFAULT_THREAD_FACTORY, LogProxyMonitor.get());
  }

  /**
   * Create a proxy server with the given Authentication schema, thread factory and monitor.
   *
   * @param auth
   *     The authentication schem to use
   * @param threadFactory
   *     The thread factory
   * @param monitor
   *     The proxy monitor
   */
  public ProxyServer(final ServerAuthenticator auth, final ThreadFactory threadFactory, final ProxyMonitor monitor) {
    this(auth, Executors.newCachedThreadPool(threadFactory), monitor);
  }

  /**
   * Create a proxy server with the given Authentication schema, thread factory and monitor.
   *
   * @param auth
   *     The authentication schem to use
   * @param executorService
   *     The service used to perform work
   * @param monitor
   *     The proxy monitor
   */
  public ProxyServer(final ServerAuthenticator auth, final ExecutorService executorService,
      final ProxyMonitor monitor) {
    this.auth = auth;
    this.monitor = monitor;
    this.executorService = executorService;
  }

  // Public methods
  // ///////////////

  /**
   * Set proxy.
   * <p/>
   * Allows Proxy chaining so that one Proxy server is connected to another
   * and so on. If proxy supports SOCKSv4, then only some SOCKSv5 requests can
   * be handled, UDP would not work, however CONNECT and BIND will be
   * translated.
   *
   * @param p
   *     Proxy which should be used to handle user requests.
   */
  public void setProxy(final SocksProxyBase p) {
    proxy = p;
    // FIXME: Side effect.
    UDPRelayServer.proxy = proxy;
  }

  /**
   * Get proxy.
   *
   * @return Proxy wich is used to handle user requests.
   */
  public SocksProxyBase getProxy() {
    return proxy;
  }

  /**
   * Sets the timeout for connections, how long shoud server wait for data to
   * arrive before dropping the connection.<br>
   * Zero timeout implies infinity.<br>
   * Default timeout is 3 minutes.
   */
  public void setIdleTimeout(final int timeout) {
    idleTimeout = timeout;
  }

  /**
   * Sets the timeout for BIND command, how long the server should wait for
   * the incoming connection.<br>
   * Zero timeout implies infinity.<br>
   * Default timeout is 3 minutes.
   */
  public void setAcceptTimeout(final int timeout) {
    acceptTimeout = timeout;
  }

  /**
   * Sets the timeout for UDPRelay server.<br>
   * Zero timeout implies infinity.<br>
   * Default timeout is 3 minutes.
   */
  public void setUDPTimeout(final int timeout) {
    UDPRelayServer.setTimeout(timeout);
  }

  /**
   * Sets the size of the datagrams used in the UDPRelayServer.<br>
   * Default size is 64K, a bit more than maximum possible size of the
   * datagram.
   */
  public void setDatagramSize(final int size) {
    UDPRelayServer.setDatagramSize(size);
  }

  /**
   * Get internal state of proxy execution
   *
   * @return The current status
   */
  public ProxyStatus getProxyStatus() {
    synchronized (statusMutex) {
      return proxyStatus;
    }
  }

  private void setProxyStatus(final ProxyStatus proxyStatus) {
    synchronized (statusMutex) {
      this.proxyStatus = proxyStatus;
      statusMutex.notifyAll();
    }
  }

  public ProxyStatus awaitStartup() throws InterruptedException {
    synchronized (statusMutex) {
      while (proxyStatus == ProxyStatus.STOPED) {
        statusMutex.wait();
      }
      return proxyStatus;
    }
  }

  public int getPort() {
    synchronized (statusMutex) {
      if (ProxyStatus.STARTED != proxyStatus) {
        throw new IllegalStateException("Proxy must be started; currently " + proxyStatus);
      }
      return ss.getLocalPort();
    }
  }

  public String getHost() {
    synchronized (statusMutex) {
      if (ProxyStatus.STARTED != proxyStatus) {
        throw new IllegalStateException("Proxy must be started; currently " + proxyStatus);
      }
      return ss.getInetAddress().getHostAddress();
    }
  }

  /**
   * Start the Proxy server at given port.<br>
   * This methods blocks.
   */
  public void start(final int port) {
    start(port, 5, null);
  }

  /**
   * Create a server with the specified port, listen backlog, and local IP
   * address to bind to. The localIP argument can be used on a multi-homed
   * host for a ServerSocket that will only accept connect requests to one of
   * its addresses. If localIP is null, it will default accepting connections
   * on any/all local addresses. The port must be between 0 and 65535,
   * inclusive. <br>
   * This methods blocks.
   */
  public void start(final int port, final int backlog,
      final InetAddress localIP) {
    try {
      ss = new ServerSocket(port, backlog, localIP);
      final String address = ss.getInetAddress().getHostAddress();
      final int localPort = ss.getLocalPort();
      log.info("Starting SOCKS Proxy on: {}:{}", address, localPort);
      setProxyStatus(ProxyStatus.STARTED);

      while (true) {
        final Socket s = ss.accept();
        final String hostName = s.getInetAddress().getHostName();
        final int port2 = s.getPort();
        log.info("Accepted from:{}:{}", hostName, port2);

        final ProxyServerParams params =
            new ProxyServerParams(idleTimeout, acceptTimeout, proxy, auth, executorService, monitor);
        executorService
            .submit(new ProxyServerRunnable(params, monitor.monitor(ProxyMonitor.StreamEndpoint.CLIENT, s, null)));
      }
    }
    catch (final IOException ioe) {
      log.error("Can't start proxy", ioe);
      setProxyStatus(ProxyStatus.ERROR);
    }
    finally {
    }
  }

  /**
   * Stop server operation.It would be wise to interrupt thread running the
   * server afterwards.
   */
  public void stop() {
    try {
      if (ss != null) {
        ss.close();
      }
    }
    catch (final IOException ioe) {
      log.warn("Exception thrown closing the server socket", ioe);
    }
    finally {
      setProxyStatus(ProxyStatus.STOPED);
      executorService.shutdown();
    }

  }
}
