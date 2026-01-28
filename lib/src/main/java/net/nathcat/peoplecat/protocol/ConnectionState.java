package net.nathcat.peoplecat.protocol;

import net.nathcat.peoplecat.database.types.User;

/**
 * Used to manage the state of a connection.
 *
 */
public class ConnectionState {
  public User user;

  /**
   * Determine whether the connection is authenticated or not
   *
   * @return The connection's authentication state.
   */
  public boolean isAuthenticated() {
    return user != null;
  }
}
