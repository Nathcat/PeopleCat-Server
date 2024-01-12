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
                    handler.authenticated = true;
                    return new Packet[] {Packet.createPacket(
                            Packet.TYPE_AUTHENTICATE,
                            true,
                            dbUser
                    )};
                }
                else {
                    handler.authenticated = false;
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

            @Override
            public Packet[] getUser(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get user request does not accept multi-packet arrays.")};

                // Get the request data
                JSONObject request = packets[0].getData();
                JSONObject[] users;

                // Request the data from the SQL server
                try {
                    if (request.containsKey("ID")) {
                        users = Database.extractResultSet(((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE ID = " + request.get("ID") + ";"));

                    } else if (request.containsKey("username")) {
                        users = Database.extractResultSet(((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + request.get("username") + "%\";"));

                    } else if (request.containsKey("display_name")) {
                        users = Database.extractResultSet(((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `display_name` LIKE \"" + request.get("display_name") + "%\";"));

                    } else {
                        return new Packet[]{Packet.createError("Incorrect data provided", "You must provide at least one of the following fields, ID, username, or display_name")};
                    }
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                // In case no users are returned
                if (users.length == 0) {
                    return new Packet[] {
                            Packet.createPacket(Packet.TYPE_GET_USER, true, new JSONObject())
                    };
                }

                // Create the response packet sequence
                Packet[] response = new Packet[users.length];
                for (int i = 0; i < users.length-1; i++) {
                    users[i].remove("password");
                    response[i] = Packet.createPacket(
                            Packet.TYPE_GET_USER,
                            false,
                            users[i]
                    );
                }

                response[response.length-1] = Packet.createPacket(
                        Packet.TYPE_GET_USER,
                        true,
                        users[users.length-1]
                );

                return response;
            }
        });

        this.server = server;

        log("Got connection.");
    }

    @Override
    public void run() {
        log("Thread started.");

        this.setup();

        try {
            while (true) {
                // Get the packet sequence from the input stream
                ArrayList<Packet> packets = new ArrayList<>();
                Packet p;
                while (!(p = getPacket()).isFinal) {
                    log("Got packet:\n" + p);

                    if (p.type == Packet.TYPE_CLOSE) {
                        break;
                    }

                    packets.add(p);
                }

                log("Got packet:\n" + p);

                if (p.type == Packet.TYPE_CLOSE) {
                    break;
                }

                packets.add(p);


                Packet[] packetSequence = packets.toArray(new Packet[0]);

                // Use a response handler to determine the response from the packet sequence
                Packet[] responseSequence = packetHandler.handle(this, packetSequence);
                if (responseSequence == null) continue;

                // Send the response sequence to the client through the output stream
                for (Packet packet : responseSequence) {
                    log("Writing packet: \n" + packet + " -> " + packet.getData().toJSONString());
                    writePacket(packet);
                }
            }
        } catch (Exception ignored) {}

        log("Closing thread.");
        close();
        active = false;
        interrupt();
    }
}
