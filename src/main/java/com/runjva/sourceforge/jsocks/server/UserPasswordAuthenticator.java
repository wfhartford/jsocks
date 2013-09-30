package com.runjva.sourceforge.jsocks.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class implements SOCKS5 User/Password authentication scheme as defined
 * in rfc1929,the server side of it. (see docs/rfc1929.txt)
 */
public class UserPasswordAuthenticator extends ServerAuthenticatorBase {

  static final int METHOD_ID = 2;

  UserValidation validator;

  /**
   * Construct a new UserPasswordAuthentication object, with given
   * UserVlaidation scheme.
   *
   * @param validator
   *     UserValidation to use for validating users.
   */
  public UserPasswordAuthenticator(UserValidation validator) {
    this.validator = validator;
  }

  @Override
  public ServerAuthenticator startSession(Socket s) throws IOException {
    final InputStream in = s.getInputStream();
    final OutputStream out = s.getOutputStream();

    if (in.read() != 5) {
      return null; // Drop non version 5 messages.
    }

    if (!selectSocks5Authentication(in, out, METHOD_ID)) {
      return null;
    }
    final String username = doUserPasswordAuthentication(s, in, out);
    if (null == username) {
      return null;
    }

    return new ServerAuthenticatorNone(in, out, username);
  }

  // Private Methods
  // ////////////////

  private String doUserPasswordAuthentication(Socket s, InputStream in,
      OutputStream out) throws IOException {
    final int version = in.read();
    if (version != 1) {
      return null;
    }

    final int ulen = in.read();
    if (ulen < 0) {
      return null;
    }

    final byte[] user = new byte[ulen];
    in.read(user);
    final int plen = in.read();
    if (plen < 0) {
      return null;
    }
    final byte[] password = new byte[plen];
    in.read(password);

    final String username = new String(user);
    if (validator.isUserValid(username, new String(password), s)) {
      // System.out.println("user valid");
      out.write(new byte[] { 1, 0 });
    }
    else {
      // System.out.println("user invalid");
      out.write(new byte[] { 1, 1 });
      return null;
    }

    return username;
  }
}
