package com.nathcat.peoplecat_server;

import com.nathcat.AuthCat.AuthCat;
import com.nathcat.AuthCat.Exceptions.InvalidResponse;
import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.Database;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.sql.PreparedStatement;
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

    public ClientHandler(Server server, Socket client) throws IOException {
        super(client, null);

        this.packetHandler = createPacketHandler();

        this.server = server;

        log("Got connection.");
    }

    public ClientHandler(Server server, WebSocket client, WebSocketOutputStream os, WebSocketInputStream is) throws IOException {
        super(client, os, is, null);
        this.server = server;
        packetHandler = createPacketHandler();
    }

    private IPacketHandler createPacketHandler() {
        return new IPacketHandler() {
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
                    assert user.containsKey("Username");
                    assert user.containsKey("Password");
                } catch (AssertionError e) {
                    return new Packet[] {Packet.createError("Invalid JSON provided", "The data provided does not contain the correct data, an Auth request requires both the user's username and password to complete.")};
                }

                // Attempt to log in with AuthCat
                JSONObject authCatJSON = new JSONObject();
                authCatJSON.put("username", user.get("Username"));
                authCatJSON.put("password", user.get("Password"));


                JSONObject authCatResponse;
                try {
                    authCatResponse = AuthCat.tryLogin(authCatJSON);
                } catch (InvalidResponse | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Check the response from the service
                if (((String)authCatResponse.get("state")).contentEquals("fail")) {
                    handler.authenticated = false;
                    return new Packet[] {Packet.createError("Auth failed", (String) authCatResponse.get("message"))};
                }

                handler.authenticated = true;
                handler.user = (JSONObject) authCatResponse.get("user");
                return new Packet[] {Packet.createPacket(
                        Packet.TYPE_AUTHENTICATE,
                        true,
                        handler.user
                )};
            }

            @Override
            public Packet[] createNewUser(ConnectionHandler handler, Packet[] packets) {
                return new Packet[] { Packet.createError("Feature Deprecation", "This feature is no longer available through PeopleCat, please refer to AuthCat.") };
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

                JSONObject ac_req = new JSONObject();
                if (request.containsKey("ID")) ac_req.put("id", request.get("ID"));
                else if (request.containsKey("Username")) ac_req.put("username", request.get("Username"));
                else if (request.containsKey("FullName")) ac_req.put("fullName", request.get("FullName"));

                JSONObject response;
                try {
                    response = AuthCat.userSearch(ac_req);
                } catch (InvalidResponse | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                if (((String) response.get("state")).contentEquals("success")) {
                    ArrayList<JSONObject> u = new ArrayList<>();
                    JSONObject results = (JSONObject) response.get("results");
                    for (Object k : results.keySet()) {
                        u.add((JSONObject) results.get(k));
                    }

                    users = u.toArray(new JSONObject[0]);

                    // In case no users are returned
                    if (users.length == 0) {
                        return new Packet[]{
                                Packet.createPacket(Packet.TYPE_GET_USER, true, new JSONObject())
                        };
                    }
                }
                else {
                    return new Packet[] { Packet.createError("AuthCat error", (String) response.get("message")) };
                }

                // Create the response packet sequence
                Packet[] reply = new Packet[users.length];
                for (int i = 0; i < users.length-1; i++) {
                    users[i].remove("Password");
                    reply[i] = Packet.createPacket(
                            Packet.TYPE_GET_USER,
                            false,
                            users[i]
                    );
                }

                users[users.length-1].remove("Password");
                reply[reply.length-1] = Packet.createPacket(
                        Packet.TYPE_GET_USER,
                        true,
                        users[users.length-1]
                );

                return reply;
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

                response.get(response.size() - 1).isFinal = true;

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
                        if (!otherHandler.authenticated || !otherHandler.active) continue;

                        if ((int) otherHandler.user.get("UserID") == userID) {
                            otherHandler.writePacket(notifyPacket);
                        }
                    }
                }

                return new Packet[] {Packet.createPing()};
            }

            @Override
            public Packet[] notificationMessage(ConnectionHandler handler, Packet[] packets) {
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

            @Override
            public Packet[] changeProfilePicture(ConnectionHandler handler, Packet[] packets) {
                return new Packet[] { Packet.createError("Feature Deprecation", "This feature is no longer available through PeopleCat, please refer to AuthCat.") };
            }
        };
    }

    @Override
    public void run() {
        log("Thread started.");
        this.active = true;

        // No longer required with the new websocket library
        //this.setup();

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
