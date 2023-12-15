package com.nathcat.peoplecat_server;

import com.nathcat.peoplecat_database.Database;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

/**
 * Handles a connection to a client application.
 *
 * @author Nathan Baines
 */
public class ClientHandler extends Thread {
    public interface IRequestHandler { Packet[] handler(ClientHandler handler, Packet[] packets); }

    /**
     * An array of handler methods which handle each of the request types, which correspond to an index in this array.
     */
    public static final IRequestHandler[] requestHandlers = new IRequestHandler[] {
            (ClientHandler handler, Packet[] packets) -> null,
            (ClientHandler handler, Packet[] packets) -> new Packet[] {Packet.createPing()},  // Ping handler responds with a ping as well.
            (ClientHandler handler, Packet[] packets) -> {  // Handles authentication requests
                // Check that there is only one packet in the request
                if (packets.length > 1) {
                    return new Packet[] {Packet.createError("Invalid data type", "Auth request does not accept multi-packet arrays.")};
                }

                // Get the data from the single packet
                JSONObject user = packets[0].getData();

                // Assert that the packet data contains a username and password field
                try {
                    assert user.containsKey("username");
                    assert user.containsKey("password");
                } catch (AssertionError e) {
                    return new Packet[] {Packet.createError("Invalid JSON provided", "The data provided does not contain the correct data, an Auth request requires both the user's username and password to complete.")};
                }

                // Request all the users with the given username from the database
                ResultSet rs;
                try {
                    rs = handler.server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";");

                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                JSONObject[] records = Database.extractResultSet(rs);

                // There should only be one user with the given surname, or none
                if (records.length > 1) {
                    return new Packet[] {Packet.createError("Database error", "More than one user returned for the given username.")};
                }
                else if (records.length == 0) {
                    return new Packet[] {Packet.createError("Auth failed", "Incorrect username or password")};
                }

                // Check that the database user has the same password as the one provided by the client.
                JSONObject dbUser = records[0];

                if (dbUser.get("username").equals(user.get("username")) && dbUser.get("password").equals(user.get("password"))) {
                    return new Packet[] {Packet.createPacket(
                            Packet.TYPE_AUTHENTICATE,
                            true,
                            dbUser
                    )};
                }
                else {
                    return new Packet[] {Packet.createError("Auth failed", "Incorrect username or password")};
                }
            },

            (ClientHandler handler, Packet[] packets) -> {  // Handles create new user packets
                if (packets.length > 1) {
                    return new Packet[] {Packet.createError("Invalid data type", "New user request does not accept multi-packet arrays.")};
                }


                JSONObject user = packets[0].getData();

                // Assert that the packet data contains the required data
                try {
                    assert user.containsKey("username");
                    assert user.containsKey("password");
                    assert user.containsKey("display_name");
                } catch (AssertionError e) {
                    return new Packet[] {Packet.createError("Invalid JSON provided", "The data provided does not contain the correct data, an Auth request requires both the user's username and password to complete.")};
                }

                // Check that there are no users with the same username

                try {
                    JSONObject[] existingUsers = Database.extractResultSet(
                            handler.server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";")
                    );

                    if (existingUsers.length != 0) {
                        return new Packet[] {Packet.createError("Failed to create new user", "A user with this username already exists")};
                    }

                    handler.server.db.Update(
                            "INSERT INTO `users` (`username`, `display_name`, `password`, `time_created`) VALUES (\"" + user.get("username") + "\", \"" + user.get("display_name") + "\", \"" + user.get("password") + "\", " + new Date().getTime() + ")"
                    );

                    existingUsers = Database.extractResultSet(
                            handler.server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";")
                    );

                    if (existingUsers.length != 1) {
                        return new Packet[] {Packet.createError("Failed to create new user", "Something went wrong")};
                    }

                    return new Packet[] {Packet.createPacket(
                            Packet.TYPE_CREATE_NEW_USER,
                            true,
                            existingUsers[0]
                    )};
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            },

            (ClientHandler handler, Packet[] packets) -> null  // Handles close packets
    };

    private final Server server;
    private final Socket client;
    private OutputStream outStream;
    private InputStream inStream;
    public boolean active = true;

    public ClientHandler(Server server, Socket client) throws IOException {
        this.server = server;
        this.client = client;
        outStream = client.getOutputStream();
        inStream = client.getInputStream();

        log("Got connection.");
    }

    public void log(String message) {
        System.out.println("Handler " + threadId() + ": " + message);
    }

    @Override
    public void run() {
        log("Thread started.");

        try {
            while (true) {
                // Get the packet sequence from the input stream
                ArrayList<Packet> packets = new ArrayList<>();
                Packet p;
                while (!(p = new Packet(inStream)).isFinal) {
                    log("Got packet:\n" + p);

                    if (p.type == Packet.TYPE_CLOSE) { break; }

                    packets.add(p);

                    if (p.isFinal) break;
                }

                if (p.type == Packet.TYPE_CLOSE) { break; }

                packets.add(p);


                Packet[] packetSequence = packets.toArray(new Packet[0]);

                // Use a response handler to determine the response from the packet sequence
                Packet[] responseSequence = requestHandlers[packetSequence[0].type].handler(this, packetSequence);

                // Send the response sequence to the client through the output stream
                for (Packet packet : responseSequence) {
                    outStream.write(packet.getBytes());
                }
                outStream.flush();
            }

            log("Closing thread.");
            client.close();
            active = false;
            interrupt();

        } catch (IOException e) {
            log("Error occurred (" + e.getMessage() + "), closing thread.");
            active = false;
            interrupt();
        }
    }

    @Override
    public String toString() {
        return "Handler " + threadId();
    }
}
