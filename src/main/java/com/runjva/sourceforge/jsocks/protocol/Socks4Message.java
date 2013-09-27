package com.runjva.sourceforge.jsocks.protocol;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.runjva.sourceforge.jsocks.dns.DnsResolver;
import com.runjva.sourceforge.jsocks.dns.DnsResolverFactory;

/**
 * SOCKS4 Reply/Request message.
 */

class Socks4Message extends ProxyMessage {
  private Logger log = LoggerFactory.getLogger(Socks4Message.class);
  private static final String EMPTY_USERNAME = "";

  private byte[] msgBytes;
  private int msgLength;
  private DnsResolver dnsResolver = DnsResolverFactory.getDefaultDnsResolverInstance();

  /**
   * Server failed reply, cmd command for failed request
   */
  public Socks4Message(final int cmd) {
    super(cmd, null, 0);
    this.user = null;

    msgLength = 2;
    msgBytes = new byte[2];

    msgBytes[0] = (byte) 0;
    msgBytes[1] = (byte) command;
  }

  /**
   * Server successfull reply
   */
  public Socks4Message(final int cmd, final InetAddress ip, final int port) {
    this(0, cmd, ip, port, null);
  }

  /**
   * Client request
   */
  public Socks4Message(final int cmd, final InetAddress ip, final int port,
      final String user) {
    this(SOCKS_VERSION, cmd, ip, port, user);
  }

  /**
   * Most general constructor
   */
  public Socks4Message(final int version, final int cmd,
      final InetAddress ip, final int port, final String user) {

    super(cmd, ip, port);
    this.user = user;
    this.version = version;

    msgLength = user == null ? 8 : 9 + user.length();
    msgBytes = new byte[msgLength];

    msgBytes[0] = (byte) version;
    msgBytes[1] = (byte) command;
    msgBytes[2] = (byte) (port >> 8);
    msgBytes[3] = (byte) port;

    byte[] addr;

    if (ip != null) {
      addr = ip.getAddress();
    }
    else {
      addr = new byte[4];
      addr[0] = addr[1] = addr[2] = addr[3] = 0;
    }
    System.arraycopy(addr, 0, msgBytes, 4, 4);

    if (user != null) {
      final byte[] buf = user.getBytes();
      System.arraycopy(buf, 0, msgBytes, 8, buf.length);
      msgBytes[msgBytes.length - 1] = 0;
    }
  }

  /**
   * Initialise from the stream If clientMode is true attempts to read a
   * server response otherwise reads a client request see read for more detail
   */
  public Socks4Message(final InputStream in, final boolean clientMode)
      throws IOException {
    msgBytes = null;
    read(in, clientMode);
  }

  /**
   * Initialise from the stream If clientMode is true attempts to read a
   * server response otherwise reads a client request see read for more detail
   */
  public Socks4Message(final InputStream in, final boolean clientMode, DnsResolver dnsResolver)
      throws IOException {
    msgBytes = null;
    setDnsResolver(dnsResolver);

    read(in, clientMode);
  }

  public void setDnsResolver(DnsResolver dnsResolver) {
    this.dnsResolver = dnsResolver;
  }

  @Override
  public void read(final InputStream in) throws IOException {
    read(in, true);
  }

  @Override
  public void read(final InputStream in, final boolean clientMode)
      throws IOException {
    final DataInputStream d_in = new DataInputStream(in);
    version = d_in.readUnsignedByte();
    command = d_in.readUnsignedByte();
    if (clientMode && (command != REPLY_OK)) {
      String errMsg;
      // FIXME: Range should be replaced with cases.
      if ((command > REPLY_OK) && (command < REPLY_BAD_IDENTD)) {
        errMsg = replyMessage[command - REPLY_OK];
      }
      else {
        errMsg = "Unknown Reply Code";
      }
      throw new SocksException(command, errMsg);
    }
    port = d_in.readUnsignedShort();
    final byte[] addr = new byte[4];
    d_in.readFully(addr);

    // read username
    if (!clientMode) {
      // read stream until NULL byte(0) or end of stream
      // optimization: if the first byte is empty - no username provided
      // and no object creation
      int b = in.read();
      if (b > 0) {
        StringBuilder builder = new StringBuilder(64);// simple guess of
        builder.append(b);

        while ((b = in.read()) > 0) {
          builder.append((char) b);
        }

        user = builder.toString();
      }
      else {
        user = EMPTY_USERNAME;
      }
    }

    // assign ip and host
    // check if addred is 0.0.0.x
    if (isInadmissibleIp(addr)) {
      // read until NULL byte
      StringBuilder builder = new StringBuilder(50);
      int b;
      while ((b = in.read()) > 0) {
        builder.append((char) b);
      }

      String readedHost = builder.toString();

      // for international domain name support
      if (IDN.toASCII(readedHost).length() > 255) {
        throw new IllegalArgumentException(host + " IDN: "
            + IDN.toASCII(readedHost) + " exceeds 255 char limit");
      }
      host = IDN.toASCII(readedHost);

      ip = resolveHost(host);
    }
    else {
      ip = bytes2IP(addr);
      host = ip.getHostName();
    }
  }

  private boolean isInadmissibleIp(byte[] addr) {
    boolean isInadmissibleIp = false;

    if (addr[0] == 0 && addr[1] == 0 && addr[2] == 0 && addr[3] != 0) {
      isInadmissibleIp = true;
    }

    return isInadmissibleIp;
  }

  private InetAddress resolveHost(String host) throws UnknownHostException {
    return dnsResolver.resolveByName(host);
  }

  @Override
  public void write(final OutputStream out) throws IOException {
    if (msgBytes == null) {
      final Socks4Message msg;
      msg = new Socks4Message(version, command, ip, port, user);
      msg.setDnsResolver(dnsResolver);
      msgBytes = msg.msgBytes;
      msgLength = msg.msgLength;
    }
    out.write(msgBytes);
  }

  // Class methods
  static InetAddress bytes2IP(final byte[] addr) {
    InetAddress inetAddress = null;
    try {
      inetAddress = InetAddress.getByAddress(addr);
    }
    catch (final UnknownHostException uh_ex) {

    }

    return inetAddress;
  }

  // Constants

  static final String[] replyMessage = { "Request Granted",
      "Request Rejected or Failed",
      "Failed request, can't connect to Identd",
      "Failed request, bad user name" };

  static final int SOCKS_VERSION = 4;

  public final static int REQUEST_CONNECT = 1;
  public final static int REQUEST_BIND = 2;

  public final static int REPLY_OK = 90;
  public final static int REPLY_REJECTED = 91;
  public final static int REPLY_NO_CONNECT = 92;
  public final static int REPLY_BAD_IDENTD = 93;

}
