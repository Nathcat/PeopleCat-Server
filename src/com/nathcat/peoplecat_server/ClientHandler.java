package com.nathcat.peoplecat_server;

import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.messagecat_database_entities.User;
import com.nathcat.peoplecat_database.Database;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
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
                    rs = ((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE `username` LIKE \"" + user.get("Username") + "\";");

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
                System.out.println(dbUser);

                if (dbUser.get("Username").equals(user.get("Username")) && dbUser.get("Password").equals(user.get("Password"))) {
                    handler.authenticated = true;
                    handler.user = dbUser;
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
                        users = Database.extractResultSet(((ClientHandler) handler).server.db.Select("SELECT * FROM `users` WHERE UserID = " + request.get("ID") + ";"));

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

            @Override
            public Packet[] getMessageQueue(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject request = packets[0].getData();
                if (request.get("ChatID") == null) {
                    return new Packet[] {Packet.createError("Incorrect data provided", "You must provide the ChatID.")};
                }

                MessageQueue queue = server.db.messageStore.GetMessageQueue(Math.toIntExact((long) request.get("ChatID")));  // Because for some fucking reason this is a long

                if (queue == null) {
                    return new Packet[] {Packet.createError("Chat does not exist", "The message queue for the specified chat does not exist in the database.")};
                }

                ArrayList<Packet> response = new ArrayList<>();
                Message msg;
                int i = 0;
                while ((msg = queue.Get(i)) != null) {  // I really hate my old code ;_;
                    JSONObject msgData = new JSONObject();

                    for (Field field : Message.class.getFields()) {
                        try {
                            msgData.put(field.getName(), field.get(msg));
                        } catch (IllegalAccessException ignored) {}
                    }

                    response.add(
                            Packet.createPacket(
                                    Packet.TYPE_GET_MESSAGE_QUEUE,
                                    false,
                                    msgData
                            )
                    );

                    i++;
                }

                if (response.isEmpty()) {
                    return new Packet[] {Packet.createError("No messages", "There are no messages in this chat.")};
                }

                response.getLast().isFinal = true;

                return response.toArray(new Packet[0]);
            }

            @Override
            public Packet[] sendMessage(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject request = packets[0].getData();
                Message msg = new Message((int) handler.user.get("UserID"), Math.toIntExact((long) request.get("ChatID")), (long) request.get("TimeSent"), request.get("Content"));
                server.db.messageStore.GetMessageQueue(msg.ChatID).Push(msg);
                try {
                    server.db.messageStore.WriteToFile();
                }
                catch (IOException e) {
                    return new Packet[] {Packet.createError("Server error", "An error occurred when writing the message store to the disk.")};
                }

                // Notify other users about this message
                int chatID = Math.toIntExact((long) request.get("ChatID"));
                JSONObject notification = new JSONObject();
                notification.put("title", "New message");
                notification.put("message", "You have a new message from ");
                notification.put("ChatID", chatID);
                Packet notifyPacket = Packet.createPacket(Packet.TYPE_NOTIFICATION_MESSAGE, true, notification);
                for (int userID : server.db.chatMemberships.get(chatID)) {
                    if (userID == (int) handler.user.get("UserID")) {
                        continue;
                    }

                    for (ConnectionHandler otherHandler : server.handlers) {
                        if ((int) otherHandler.user.get("UserID") == userID) {
                            otherHandler.writePacket(notifyPacket);
                        }
                    }
                }

                return new Packet[] {Packet.createPing()};
            }

            @Override
            public Packet[] notifitcationMessage(ConnectionHandler handler, Packet[] packets) {
                return new Packet[] {Packet.createError("Invalid packet type", "The server is not able to receive message notification packets.")};
            }

            @Override
            public Packet[] joinChat(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject request = packets[0].getData();
                JSONObject chat;
                try {
                    JSONObject[] results = Database.extractResultSet(server.db.Select("select * from chats where ChatID = " + request.get("ChatID") + ";"));

                    if (results.length != 1) {
                        return new Packet[] {Packet.createError("Database error", "Could not find the specified chat or multiple chats exist with this ID.")};
                    }

                    chat = results[0];
                }
                catch (Exception e) {
                    return new Packet[] {Packet.createError("Server error", e.getMessage())};
                }

                if (!chat.get("JoinCode").equals(request.get("JoinCode"))) {
                    return new Packet[] {Packet.createError("Invalid join code", "The given join code is incorrect.")};
                }

                int chatID = Math.toIntExact((long) request.get("ChatID"));
                int[] members = server.db.chatMemberships.get(chatID);
                for (int m : members) {
                    if (m == (int) handler.user.get("UserID")) {
                        return new Packet[] {Packet.createError("Already member", "You are already a member of this chat.")};
                    }
                }

                int[] newMembers = new int[members.length + 1];
                System.arraycopy(members, 0, newMembers, 0, members.length);
                newMembers[members.length] = (int) handler.user.get("UserID");
                server.db.chatMemberships.set(chatID, newMembers);

                return new Packet[] {Packet.createPacket(Packet.TYPE_JOIN_CHAT, true, chat)};
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
                    if (packet.type != Packet.TYPE_PING) log("Writing packet: \n" + packet + " -> " + packet.getData().toJSONString());
                    else log("Pinging client.");
                    writePacket(packet);
                }
            }
        } catch (Exception e) { log("\033[91;3m" + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m"); }

        log("Closing thread.");
        close();
        active = false;
        interrupt();
    }
}
