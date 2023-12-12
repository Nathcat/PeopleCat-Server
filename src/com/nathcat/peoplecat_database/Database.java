package com.nathcat.peoplecat_database;

import com.mysql.cj.jdbc.exceptions.CommunicationsException;
import com.nathcat.messagecat_database.KeyStore;
import com.nathcat.messagecat_database.MessageStore;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.*;
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
     */
    public FriendshipStore friendshipStore;
    /**
     * The key store on this database
     */
    public KeyStore keyStore;
    /**
     * The message store on this database
     */
    public MessageStore messageStore;

    public Database() throws FileNotFoundException, ParseException, SQLException {
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

}
