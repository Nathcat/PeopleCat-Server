package net.nathcat.peoplecat.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Wrapper which can form connections to a remote SQL database.
 * Ensures that a connection is restarted should it time out, otherwise any
 * SQLExceptions are exposed to be handled outside of this class.
 */
public class DBConnection {
  private Connection conn;
  private final String url;
  private final String user;
  private final String pswd;

  public DBConnection(String url, String user, String pswd) throws SQLException {
    this.url = url;
    this.user = user;
    this.pswd = pswd;

    startConnection();
  }

  /**
   * Start a connection to the database
   */
  private void startConnection() throws SQLException {
    conn = DriverManager.getConnection(url, user, pswd);
  }

  /**
   * Create a prepared statement on the established connection;
   * 
   * @param q The query
   * @return A prepared statement with q as the query
   */
  public PreparedStatement prepare(String q) {
    try {
      return conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
    } catch (SQLException e) {
      System.err.println("SQL Exception occurred when preparing statement: " + e.getMessage());
      try {
        startConnection();
      } catch (SQLException e1) {
        System.err.println("Failed to create connection to DB.");
        e.printStackTrace();
      }

      return prepare(q);
    }
  }

  /**
   * Get the last generated key from a statement
   *
   * @param stmt The statement to get a key from
   * @return The last generated key by the statement
   * @throws SQLException
   */
  public static int getLastKey(PreparedStatement stmt) throws SQLException {
    ResultSet keys = stmt.getGeneratedKeys();
    keys.next();
    return Math.toIntExact(keys.getLong(1));
  }
}
