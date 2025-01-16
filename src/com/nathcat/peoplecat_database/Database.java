package com.nathcat.peoplecat_database;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import com.nathcat.messagecat_database.KeyStore;
import com.nathcat.messagecat_database.MessageStore;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Database {
    /**
     * The server config data
     */
    private final JSONObject config;
    /**
     * The currently active server connection
     */
    private Connection conn;
    /**
     * The friendship store on this database
     * @deprecated friendship store will be moved to an SQL table with the AuthCat integration
     */
    public DataStore<Integer, int[]> friendshipStore; // = new DataStore<>("Assets/Data/Friendships.bin");
    /**
     * The local store for chat memberships
     * @deprecated this will be migrated to an SQL table from version 5.0.0
     */
    public DataStore<Integer, int[]> chatMemberships; // = new DataStore<>("Assets/Data/ChatMemberships.bin");
    /**
     * The key store on this database
     */
    public KeyStore keyStore;
    /**
     * The message store on this database
     * @deprecated Should no longer be used, physical message handling has been moved to <code>MessageBox</code>
     */
    public MessageStore messageStore = new MessageStore();

    public Database() throws IOException, ParseException, SQLException {
        config = getConfig();
        StartMySQLConnection();
    }

    /**
     * Get the config JSON for the database
     * @return JSONObject containing the config data for the database
     * @throws FileNotFoundException Thrown if the db cannot find the config file
     * @throws ParseException Thrown if the config file content is not valid JSON
     */
    public static JSONObject getConfig() throws FileNotFoundException, ParseException {
        Scanner fileIn = new Scanner(new File("Assets/MySQL_Config.json"));
        StringBuilder sb = new StringBuilder();
        while (fileIn.hasNextLine()) {
            sb.append(fileIn.nextLine());
        }

        return (JSONObject) (new JSONParser().parse(sb.toString()));
    }

    /**
     * Start a new connection to the MySQL Server
     * @throws SQLException Thrown if the db cannot connect to the MySQL server correctly
     */
    private void StartMySQLConnection() throws SQLException {
        // Create a connection to the MySQL database.
        conn = DriverManager.getConnection((String) config.get("connection_url_peoplecat"), (String) config.get("username"), (String) config.get("password"));
    }

    /**
     * Perform a Select query on the database.
     * @param query The query to be executed
     * @return The ResultSet returned from the query
     * @throws SQLException Thrown by SQL errors.
     * @deprecated Use prepared statements
     */
    public ResultSet Select(String query) throws SQLException {
        try {
            // Create and execute the statement
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.execute(query);
            // Get the result set and close the statement
            ResultSet rs = stmt.getResultSet();

            // Return the result set
            return rs;

        } catch (CommunicationsException e) {
            // Restart the connection and try again
            StartMySQLConnection();

            // Create and execute the statement
            Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY);
            stmt.execute(query);
            // Get the result set and close the statement
            ResultSet rs = stmt.getResultSet();

            // Return the result set
            return rs;
        }
    }

    /**
     * Perform an update query on the database (or any query that does not have a result set)
     * @param query The query to be executed
     * @throws SQLException Thrown by SQL errors
     * @deprecated Use prepared statements
     */
    public void Update(String query) throws SQLException {
        try {
            // Create and execute the statement
            Statement stmt = conn.createStatement();
            stmt.execute(query);

            // Close the statement
            stmt.close();

        } catch (CommunicationsException e) {
            // Restart the connection and try again
            StartMySQLConnection();

            // Create and execute the statement
            Statement stmt = conn.createStatement();
            stmt.execute(query);

            // Close the statement
            stmt.close();
        }
    }

    /**
     * Create a prepared SQL statement
     * @param q The query to prepare
     * @return The <code>PreparedStatement</code> object
     * @throws SQLException Thrown in case of invalid SQL or some other error
     */
    public PreparedStatement getPreparedStatement(String q) throws SQLException {
        try {
            if (conn.isClosed()) StartMySQLConnection();
            return conn.prepareStatement(q, Statement.RETURN_GENERATED_KEYS);
        }
        catch (CommunicationsException e) {
            StartMySQLConnection();
            return getPreparedStatement(q);
        }
    }

    /**
     * Transform an SQL result set into an array of JSONObjects representing each record.
     * @param rs The result set to transform
     * @return The resulting array of JSONObjects
     */
    public static JSONObject[] extractResultSet(ResultSet rs) {
        try {
            ResultSetMetaData meta = rs.getMetaData();
            ArrayList<JSONObject> result = new ArrayList<>();

            while (rs.next()) {
                JSONObject row = new JSONObject();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    row.put(meta.getColumnLabel(i + 1), rs.getObject(meta.getColumnLabel(i + 1)));
                }

                result.add(row);
            }

            return result.toArray(new JSONObject[0]);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
