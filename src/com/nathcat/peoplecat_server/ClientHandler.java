package com.nathcat.peoplecat_server;

import com.mysql.cj.x.protobuf.MysqlxPrepare;
import com.nathcat.AuthCat.AuthCat;
import com.nathcat.AuthCat.Exceptions.InvalidResponse;
import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_database.MessageBox;
import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.nio.file.FileSystemNotFoundException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

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
                JSONObject d = packets[0].getData();

                handler.log("\033[91;3mError from client:\n" + d.get("name") + ":\n" + d.get("msg") + "\033[0m");
                return new Packet[0];
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

                if (user.containsKey("cookieAuth")) {
                    String cookie = (String) user.get("cookieAuth");
                    JSONObject r;
                    try {
                        r = AuthCat.loginWithCookie(cookie);

                    } catch (IOException | InterruptedException | InvalidResponse e) {
                        throw new RuntimeException(e);
                    }

                    if (r != null) {
                        handler.authenticated = true;
                        handler.user = r;
                        handler.user.put("id", Math.toIntExact((long) handler.user.get("id")));
                        ClientHandler ch = (ClientHandler) handler;

                        List<ClientHandler> handlerList = ch.server.userToHandler.get((int) handler.user.get("id"));
                        if (handlerList == null) {
                            handlerList = Collections.synchronizedList(new LinkedList<>());
                            handlerList.add(ch);
                            ch.server.userToHandler.put((int) handler.user.get("id"), handlerList);
                        }
                        else {
                            handlerList.add(ch);
                        }

                        return new Packet[] {Packet.createPacket(
                                Packet.TYPE_AUTHENTICATE,
                                true,
                                handler.user
                        )};
                    }

                    // If we get here, then cookie authentication has failed, and we will attempt normal credential
                    // authentication.
                }

                // Assert that the packet data contains a username and password field
                try {
                    assert user.containsKey("username");
                    assert user.containsKey("password");
                } catch (AssertionError e) {
                    return new Packet[] {Packet.createError("Invalid JSON provided", "The data provided does not contain the correct data, an Auth request requires both the user's username and password to complete.")};
                }

                // Attempt to log in with AuthCat
                JSONObject authCatResponse;
                try {
                    authCatResponse = AuthCat.tryLogin(user);
                } catch (InvalidResponse | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                handler.log("Got response from AuthCat: " + authCatResponse.toJSONString());

                // Check the response from the service
                if (((String)authCatResponse.get("status")).contentEquals("fail")) {
                    handler.authenticated = false;
                    return new Packet[] {Packet.createError("Auth failed", (String) authCatResponse.get("message"))};
                }

                handler.authenticated = true;
                handler.user = (JSONObject) authCatResponse.get("user");
                handler.user.put("id", Math.toIntExact((long) handler.user.get("id")));
                ClientHandler ch = (ClientHandler) handler;
                List<ClientHandler> handlerList = ch.server.userToHandler.get((int) handler.user.get("id"));
                if (handlerList != null) handlerList.add(ch);
                else {
                    handlerList = Collections.synchronizedList(new LinkedList<>());
                    handlerList.add(ch);
                    ch.server.userToHandler.put((int) handler.user.get("id"), handlerList);
                }

                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("SELECT follower FROM Friends WHERE id = ?");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.execute();
                    JSONObject[] r = Database.extractResultSet(stmt.getResultSet());
                    JSONObject user_notif_data = new JSONObject();
                    user_notif_data.putAll(handler.user);
                    user_notif_data.remove("password");
                    user_notif_data.remove("verified");
                    user_notif_data.remove("email");

                    for (JSONObject jsonObject : r) {
                        //ClientHandler h = server.userToHandler.get((int) jsonObject.get("follower"));
                        List<ClientHandler> followerList = server.userToHandler.get((int) jsonObject.get("follower"));

                        if (followerList != null) followerList.forEach((ClientHandler h) -> h.writePacket(
                               Packet.createPacket(
                                       Packet.TYPE_NOTIFICATION_USER_ONLINE,
                                       true,
                                       user_notif_data
                               )
                        ));
                    }
                }
                catch (SQLException e) {
                    handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                }

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

                JSONObject response;
                try {
                    response = AuthCat.userSearch(request);
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
                if (request.get("chatId") == null) {
                    return new Packet[] {Packet.createError("Incorrect data provided", "You must provide the ChatID.")};
                }

                int chatId = Math.toIntExact((long) request.get("chatId"));
                Message[] messages;
                try {
                    messages = MessageBox.openMessageBox(chatId);
                }
                catch (FileNotFoundException e) {
                    try {
                        MessageBox.updateMessageBox(chatId, new Message[0]);
                        messages = MessageBox.openMessageBox(chatId);
                    }
                    catch (IOException ex) { throw new RuntimeException(ex); }
                }
                catch (IOException e) { throw new RuntimeException(e); }

                ArrayList<Packet> response = new ArrayList<>();
                JSONObject preResponse = new JSONObject();
                preResponse.put("messageCount", messages.length);

                response.add(Packet.createPacket(
                        Packet.TYPE_GET_MESSAGE_QUEUE,
                        false,
                        preResponse
                ));

                for (JSONObject m : Arrays.stream(messages).map(MessageBox::messageToJSON).toList()) {
                    response.add(Packet.createPacket(
                            Packet.TYPE_GET_MESSAGE_QUEUE,
                            false,
                            m
                    ));
                }

                if (messages.length == 0) {
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
                Message msg = new Message((int) handler.user.get("id"), Math.toIntExact((long) request.get("chatId")), (long) request.get("timeSent"), request.get("content"));

                try {
                    Message[] messages = MessageBox.openMessageBox(msg.ChatID);
                    Message[] newMessages = new Message[messages.length + 1];
                    System.arraycopy(messages, 0, newMessages, 0, messages.length);
                    newMessages[messages.length] = msg;
                    MessageBox.updateMessageBox(msg.ChatID, newMessages);
                }
                catch (IOException e) {
                    return new Packet[] {Packet.createError("Server error", "An error occurred when writing the message store to the disk.")};
                }

                // Notify other users about this message
                int chatID = Math.toIntExact((long) request.get("chatId"));
                JSONObject notification = new JSONObject();
                notification.put("chatId", chatID);
                JSONObject msgJSON = MessageBox.messageToJSON(msg);
                notification.put("message", msgJSON);
                Packet notifyPacket = Packet.createPacket(Packet.TYPE_NOTIFICATION_MESSAGE, true, notification);
                ClientHandler ch = (ClientHandler) handler;

                int[] members;
                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("SELECT `user` FROM ChatMemberships WHERE `chatid` = ?");
                    stmt.setInt(1, chatID);
                    stmt.execute();

                    JSONObject[] results = Database.extractResultSet(stmt.getResultSet());
                    members = new int[results.length];
                    for (int i = 0; i < results.length; i++) {
                        members[i] = (int) results[i].get("user");
                    }
                }
                catch (SQLException e) {
                    handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
                    return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
                }

                for (int userID : members) {
                    // Removing this condition will allow multiple clients connected under the same user
                    // to receive messages from each other.

                    //if (userID == (int) handler.user.get("id")) {
                    //    continue;
                    //}

                    List<ClientHandler> handlerList = ch.server.userToHandler.get(userID);
                    if (handlerList != null) handlerList.forEach((ClientHandler h) -> h.writePacket(notifyPacket));
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
                    JSONObject[] results;
                    PreparedStatement stmt = server.db.getPreparedStatement("SELECT * FROM Chats WHERE ChatID = ?");
                    stmt.setInt(1, (int) ((long) request.get("chatId")));
                    stmt.execute(); results = Database.extractResultSet(stmt.getResultSet());

                    if (results.length != 1) {
                        return new Packet[] {Packet.createError("Database error", "Could not find the specified chat or multiple chats exist with this ID.")};
                    }

                    chat = results[0];
                }
                catch (Exception e) {
                    return new Packet[] {Packet.createError("Server error", e.getMessage())};
                }

                if (!chat.get("JoinCode").equals(request.get("joinCode"))) {
                    return new Packet[] {Packet.createError("Invalid join code", "The given join code is incorrect.")};
                }

                int chatID = Math.toIntExact((long) request.get("chatId"));

                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("INSERT INTO ChatMemberships (`user`, `chatid`) VALUES (?, ?)");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.setInt(2, chatID);
                    stmt.executeUpdate();
                } catch (SQLException e) {
                    if (e.getClass().getName().contentEquals(SQLIntegrityConstraintViolationException.class.getName())) {
                        return new Packet[] {Packet.createError("Already member", "You are already a member of this chat.")};
                    }
                    else {
                        handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
                        return new Packet[] { Packet.createError("SQL Error", e.getMessage()) };
                    }
                }

                JSONObject chatJSON = new JSONObject();
                chatJSON.put("chatId", chat.get("ChatID"));
                chatJSON.put("name", chat.get("Name"));
                chatJSON.put("keyId", chat.get("KeyID"));
                chatJSON.put("joinCode", chat.get("JoinCode"));
                chatJSON.put("icon", chat.get("Icon"));

                return new Packet[] {Packet.createPacket(Packet.TYPE_JOIN_CHAT, true, chatJSON)};
            }

            @Override
            public Packet[] changeProfilePicture(ConnectionHandler handler, Packet[] packets) {
                return new Packet[] { Packet.createError("Feature Deprecation", "This feature is no longer available through PeopleCat, please refer to AuthCat.") };
            }

            @Override
            public Packet[] getActiveUserCount(ConnectionHandler handler, Packet[] packets) {
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject d = new JSONObject();
                d.put("usersOnline", server.handlers.size());

                return new Packet[] { Packet.createPacket(
                        Packet.TYPE_GET_ACTIVE_USER_COUNT,
                        true,
                        d
                ) };
            }

            @Override
            public Packet[] notificationUserOnline(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] notificationUserOffline(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getFriends(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject[] results;
                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("SELECT u.username, u.fullName, u.pfpPath FROM Friends LEFT JOIN SSO.Users as u ON Friends.id = u.id WHERE Friends.id = ?");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.execute();

                    results = Database.extractResultSet(stmt.getResultSet());
                    stmt.close();

                } catch (SQLException e) {
                    handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                    return new Packet[] { Packet.createError("Database error", e.getMessage()) };
                }

                Packet[] response = new Packet[results.length];
                for (int i = 0; i < results.length; i++) {
                    response[i] = Packet.createPacket(
                            Packet.TYPE_GET_FRIENDS,
                            false,
                            results[i]
                    );
                }

                response[response.length - 1].isFinal = true;
                return response;
            }

            @Override
            public Packet[] friendRequest(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject data = packets[0].getData();
                String action = (String) data.get("action");
                Packet[] response;

                if (action.contentEquals("SEND")) {
                    if (!data.containsKey("recipient")) {
                        return new Packet[] { Packet.createError("Invalid request", "This action type must contain the recipient field.") };
                    }

                    try {
                        PreparedStatement stmt = server.db.getPreparedStatement("INSERT INTO FriendRequests (sender, recipient) values (?, ?)");
                        stmt.setInt(1, (int) handler.user.get("id"));
                        stmt.setInt(2, (int) data.get("recipient"));
                        stmt.executeUpdate();
                        stmt.close();

                        stmt = server.db.getPreparedStatement("SELECT id FROM FriendRequests WHERE sender = ? AND recipient = ?");
                        stmt.setInt(1, (int) handler.user.get("id"));
                        stmt.setInt(2, (int) data.get("recipient"));
                        stmt.execute();

                        JSONObject[] r = Database.extractResultSet(stmt.getResultSet());
                        stmt.close();

                        response = new Packet[] { Packet.createPacket(Packet.TYPE_FRIEND_REQUEST, true, r[0]) };
                    }
                    catch (SQLException e) {
                        handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                        return new Packet[] { Packet.createError("Database error", e.getMessage()) };
                    }
                }
                else if (action.contentEquals("ACCEPT")) {
                    if (!data.containsKey("id")) {
                        return new Packet[] { Packet.createError("Invalid request", "This action type must contain the id field.") };
                    }

                    try {
                        PreparedStatement stmt = server.db.getPreparedStatement("SELECT sender FROM FriendRequests WHERE id = ?");
                        stmt.setInt(1, (int) data.get("id"));
                        stmt.execute();
                        int sender;
                        try {
                            sender = (int) Database.extractResultSet(stmt.getResultSet())[0].get("sender");
                        }
                        catch (Exception e) {
                            // Problem is likely that the request does not exist
                            stmt.close();
                            return new Packet[] { Packet.createError("Friend request does not exist", e.getMessage()) };
                        }

                        stmt.close();

                        stmt = server.db.getPreparedStatement("INSERT INTO Friends (id, follower) values (?, ?), (?, ?)");
                        stmt.setInt(1, (int) handler.user.get("id"));
                        stmt.setInt(2, sender);
                        stmt.setInt(3, sender);
                        stmt.setInt(4, (int) handler.user.get("id"));
                        stmt.executeUpdate();
                        stmt.close();

                        stmt = server.db.getPreparedStatement("DELETE FROM FriendRequests WHERE id = ?");
                        stmt.setInt(1, (int) data.get("id"));
                        stmt.executeUpdate();
                        stmt.close();
                    }
                    catch (SQLException e) {
                        handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                        return new Packet[] { Packet.createError("Database error", e.getMessage()) };
                    }

                    response = new Packet[0];
                }
                else if (action.contentEquals("DECLINE")) {
                    if (!data.containsKey("id")) {
                        return new Packet[] { Packet.createError("Invalid request", "This action type must contain the id field.") };
                    }

                    try {
                        PreparedStatement stmt = server.db.getPreparedStatement("DELETE FROM FriendRequests WHERE id = ?");
                        stmt.setInt(1, (int) data.get("id"));
                        stmt.executeUpdate();
                        stmt.close();
                    }
                    catch (SQLException e) {
                        handler.log("\033[91;3mSQL error! " + e.getMessage());
                        return new Packet[] { Packet.createError("Database error", e.getMessage()) };
                    }

                    response = new Packet[0];
                }
                else if (action.contentEquals("GET")) {
                    try {
                        PreparedStatement stmt = server.db.getPreparedStatement("SELECT id, sender FROM FriendRequests WHERE recipient = ?");
                        stmt.setInt(1, (int) handler.user.get("id"));
                        stmt.execute();
                        JSONObject[] r = Database.extractResultSet(stmt.getResultSet());

                        response = new Packet[r.length];
                        for (int i = 0; i < r.length; i++) {
                            response[i] = Packet.createPacket(
                                    Packet.TYPE_FRIEND_REQUEST,
                                    false,
                                    r[i]
                            );
                        }

                        response[response.length - 1].isFinal = true;
                    }
                    catch (SQLException e) {
                        handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
                        return new Packet[] { Packet.createError("Database error", e.getMessage()) };
                    }
                }
                else {
                    response = new Packet[] { Packet.createError("Unrecognised friend request action", "The action \"" + action + "\" is not recognised.") };
                }

                return response;
            }

            @Override
            public Packet[] getServerInfo(ConnectionHandler handler, Packet[] packets) {
                JSONObject d = new JSONObject();
                d.put("version", Server.version);
                d.put("serverTime", new Date().toString());

                return new Packet[] { Packet.createPacket(
                        Packet.TYPE_GET_SERVER_INFO,
                        true,
                        d
                )};
            }

            @Override
            public Packet[] getChatMemberships(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                Packet[] stream;
                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("SELECT Chats.ChatID AS `chatId`, Name AS `name`, KeyID AS `keyId`, JoinCode AS `joinCode`, Icon AS `icon` FROM ChatMemberships INNER JOIN Chats ON ChatMemberships.`chatid` = Chats.ChatID WHERE `user` = ?");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.execute();
                    JSONObject[] results = Database.extractResultSet(stmt.getResultSet());

                    if (results.length == 0) {
                        return new Packet[] { Packet.createError("No Chat Memberships", "This user is not a member of any chats.") };
                    }

                    stream = new Packet[results.length];
                    for (int i = 0; i < results.length; i++) {
                        stream[i] = Packet.createPacket(
                                Packet.TYPE_GET_CHAT_MEMBERSHIPS,
                                false,
                                results[i]
                        );
                    }
                }
                catch (SQLException e) {
                    handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
                    return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
                }

                stream[stream.length - 1].isFinal = true;
                return stream;
            }

            @Override
            public Packet[] createChat(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                JSONObject request = packets[0].getData();
                if (!request.containsKey("name")) {
                    return new Packet[] { Packet.createError("Invalid Format", "You must specify the name field!") };
                }
                else if (request.get("name").equals("")) {
                    return new Packet[] { Packet.createError("Invalid Format", "The name field cannot be empty!") };
                }

                String name = (String) request.get("name");
                String icon = (String) request.get("icon");
                JSONObject chat;

                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("INSERT INTO Chats (Name, JoinCode" + (icon == null ? "" : ", Icon") + ") VALUES (?, UUID()" + (icon == null ? "" : ", ?") + ")");
                    stmt.setString(1, name);
                    if (icon != null) stmt.setString(2, icon);
                    stmt.executeUpdate();

                    stmt = server.db.getPreparedStatement("SELECT ChatID AS `chatId`, Name AS `name`, KeyID AS `keyId`, JoinCode AS `joinCode`, Icon AS `icon` FROM Chats WHERE ChatID = LAST_INSERT_ID()");
                    stmt.execute();
                    chat = Database.extractResultSet(stmt.getResultSet())[0];
                    handler.log(chat.toJSONString());

                    stmt = server.db.getPreparedStatement("INSERT INTO ChatMemberships (`user`, `chatid`) VALUES (?, ?)");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.setInt(2, (int) chat.get("chatId"));
                    stmt.executeUpdate();
                }
                catch (SQLException e) {
                    handler.log("\033[91m;3mSQL Error! " + e.getClass().getName() + " " + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()));
                    return new Packet[] { Packet.createError("Database Error", e.getMessage()) };
                }

                return new Packet[] {
                        Packet.createPacket(
                                Packet.TYPE_CREATE_CHAT,
                                true,
                                chat
                        )
                };
            }

            @Override
            public Packet[] initUserKey(ConnectionHandler handler, Packet[] packets) {
                if (!handler.authenticated) return new Packet[] {Packet.createError("Not authenticated", "This request requires you to have an authenticated connection.")};
                if (packets.length > 1) return new Packet[] {Packet.createError("Invalid data type", "Get message queue request does not accept multi-packet arrays.")};

                // Verify the request format is correct
                JSONObject request = packets[0].getData();

                if (!request.containsKey("newPublicKey")) {
                    return new Packet[] { Packet.createError("Invalid Format", "You must specify the newPublicKey field!") };
                }

                //
                // Phase 1 - Re-initialise the user's key set
                //

                try {
                    KeyManager.initUserKey((int) handler.user.get("id"), (JSONObject) request.get("newPublicKey"));

                } catch (IOException e) {
                    handler.log("\033[91m;3mKey Init Error (Phase 1): " + e.getMessage() + "\033[0m");
                    return new Packet[] { Packet.createError("Key Init Error", e.getMessage()) };
                }

                //
                // Phase 2 - Update SQL of changes
                //

                try {
                    PreparedStatement stmt = server.db.getPreparedStatement("DELETE FROM ChatMemberships WHERE user = ?");
                    stmt.setInt(1, (int) handler.user.get("id"));
                    stmt.executeUpdate();
                    stmt.close();

                } catch (SQLException e) {
                    handler.log("\033[91m;3mKey Init Error (Phase 2): " + e.getMessage() + "\033[0m");
                    return new Packet[] { Packet.createError("Key Init Error", e.getMessage()) };
                }

                return new Packet[] { Packet.createPacket(Packet.TYPE_INIT_USER_KEY, true, null) };
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

    @Override
    public void close() {
        super.close();

        if (authenticated) {
            List<ClientHandler> handlerList = server.userToHandler.get((int) user.get("id"));

            for (int i = 0; i < handlerList.size(); i++) {
                // Presumably this comparison should determine if the handlers in question are the same handlers.
                // I'm not sure why simply comparing the references doesn't work, but I will give this a go and
                // see if it works.
                // Apparantly it does indeed work now !
                if (handlerList.get(i).threadId() == this.threadId()) {
                    handlerList.remove(i);
                    break;
                }
            }

            try {
                PreparedStatement stmt = server.db.getPreparedStatement("SELECT follower FROM Friends WHERE id = ?");
                stmt.setInt(1, (int) user.get("id"));
                stmt.execute();
                JSONObject[] r = Database.extractResultSet(stmt.getResultSet());
                JSONObject user_notif_data = new JSONObject();
                user_notif_data.putAll(user);
                user_notif_data.remove("password");
                user_notif_data.remove("verified");
                user_notif_data.remove("email");

                for (JSONObject jsonObject : r) {
                    List<ClientHandler> followerList = server.userToHandler.get((int) jsonObject.get("follower"));

                    if (followerList != null)
                        followerList.forEach((ClientHandler h) -> h.writePacket(
                            Packet.createPacket(
                                    Packet.TYPE_NOTIFICATION_USER_OFFLINE,
                                    true,
                                    user_notif_data
                            )
                        ));
                }
            }
            catch (SQLException e) {
                log("\033[91;3mSQL error! " + e.getMessage());
            }
        }
    }
}
