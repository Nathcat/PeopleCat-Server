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
public class ClientHandler extends ConnectionHandler {
    private final Server server;
    public boolean active = true;

    public ClientHandler(Server server, Socket client) throws IOException {
        super(client, new IPacketHandler() {
            @Override
            public Packet[] error(ConnectionHandler handler, Packet[] packets) {
                return null;
            }

            @Override
            public Packet[] ping(ConnectionHandler handler, Packet[] packets) {
                return new Packet[] { Packet.createPing() };
            }

            @Override
            public Packet[] authenticate(ConnectionHandler handler, Packet[] packets) {
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
                    rs = ((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";");

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
            }

            @Override
            public Packet[] createNewUser(ConnectionHandler handler, Packet[] packets) {
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
                            ((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";")
                    );

                    if (existingUsers.length != 0) {
                        return new Packet[] {Packet.createError("Failed to create new user", "A user with this username already exists")};
                    }

                    ((ClientHandler) handler).server.db.Update(
                            "INSERT INTO `users` (`username`, `display_name`, `password`, `time_created`) VALUES (\"" + user.get("username") + "\", \"" + user.get("display_name") + "\", \"" + user.get("password") + "\", " + new Date().getTime() + ")"
                    );

                    existingUsers = Database.extractResultSet(
                            ((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("username") + "\";")
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
            }

            @Override
            public Packet[] close(ConnectionHandler handler, Packet[] packets) {
                return null;
            }
        });

        this.server = server;

        log("Got connection.");
    }

    @Override
    public void run() {
        log("Thread started.");

        while (true) {
            // Get the packet sequence from the input stream
            ArrayList<Packet> packets = new ArrayList<>();
            Packet p;
            while (!(p = getPacket()).isFinal) {
                log("Got packet:\n" + p);

                if (p.type == Packet.TYPE_CLOSE) { break; }

                packets.add(p);
            }

            if (p.type == Packet.TYPE_CLOSE) { break; }

            packets.add(p);


            Packet[] packetSequence = packets.toArray(new Packet[0]);

            // Use a response handler to determine the response from the packet sequence
            Packet[] responseSequence = packetHandler.handle(this, packetSequence);
            if (responseSequence == null) continue;

            // Send the response sequence to the client through the output stream
            for (Packet packet : responseSequence) {
                writePacket(packet);
            }
        }

        log("Closing thread.");
        close();
        active = false;
        interrupt();
    }
}
