package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.net.Socket;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_server.handlers.AddToChat;
import com.nathcat.peoplecat_server.handlers.Authenticate;
import com.nathcat.peoplecat_server.handlers.Close;
import com.nathcat.peoplecat_server.handlers.CreateChat;
import com.nathcat.peoplecat_server.handlers.FriendRequest;
import com.nathcat.peoplecat_server.handlers.GetActiveUserCount;
import com.nathcat.peoplecat_server.handlers.GetChatMemberships;
import com.nathcat.peoplecat_server.handlers.GetFriends;
import com.nathcat.peoplecat_server.handlers.GetMessageQueue;
import com.nathcat.peoplecat_server.handlers.GetServerInfo;
import com.nathcat.peoplecat_server.handlers.GetUser;
import com.nathcat.peoplecat_server.handlers.GetUserKey;
import com.nathcat.peoplecat_server.handlers.InitUserKey;
import com.nathcat.peoplecat_server.handlers.JoinChat;
import com.nathcat.peoplecat_server.handlers.Ping;
import com.nathcat.peoplecat_server.handlers.PushSubscribe;
import com.nathcat.peoplecat_server.handlers.PushUnsubscribe;
import com.nathcat.peoplecat_server.handlers.SendMessage;

/**
 * Handles a connection to a client application.
 *
 * @author Nathan Baines
 */
public class ClientHandler extends ConnectionHandler {
    private final Server server;

    public ClientHandler(Server server, Socket client) throws IOException {
        super(client, null);

        this.server = server;

        log("Got connection.");

        registerHandlers();
    }

    public ClientHandler(Server server, WebSocket client, WebSocketOutputStream os, WebSocketInputStream is)
            throws IOException {
        super(client, os, is);
        this.server = server;

        registerHandlers();
    }

    private void registerHandlers() {
        packetRouter.register(Packet.TYPE_ADD_TO_CHAT, new AddToChat());
        packetRouter.register(Packet.TYPE_AUTHENTICATE, new Authenticate());
        packetRouter.register(Packet.TYPE_CLOSE, new Close());
        packetRouter.register(Packet.TYPE_CREATE_CHAT, new CreateChat());
        packetRouter.register(Packet.TYPE_ERROR, new com.nathcat.peoplecat_server.handlers.Error());
        packetRouter.register(Packet.TYPE_FRIEND_REQUEST, new FriendRequest());
        packetRouter.register(Packet.TYPE_GET_ACTIVE_USER_COUNT, new GetActiveUserCount());
        packetRouter.register(Packet.TYPE_GET_CHAT_MEMBERSHIPS, new GetChatMemberships());
        packetRouter.register(Packet.TYPE_GET_FRIENDS, new GetFriends());
        packetRouter.register(Packet.TYPE_GET_MESSAGE_QUEUE, new GetMessageQueue());
        packetRouter.register(Packet.TYPE_GET_SERVER_INFO, new GetServerInfo());
        packetRouter.register(Packet.TYPE_GET_USER, new GetUser());
        packetRouter.register(Packet.TYPE_GET_USER_KEY, new GetUserKey());
        packetRouter.register(Packet.TYPE_INIT_USER_KEY, new InitUserKey());
        packetRouter.register(Packet.TYPE_JOIN_CHAT, new JoinChat());
        packetRouter.register(Packet.TYPE_PING, new Ping());
        packetRouter.register(Packet.TYPE_PUSH_SUBSCRIBE, new PushSubscribe());
        packetRouter.register(Packet.TYPE_PUSH_UNSUBSCRIBE, new PushUnsubscribe());
        packetRouter.register(Packet.TYPE_SEND_MESSAGE, new SendMessage());
    }

    @Override
    public void run() {
        log("Thread started.");
        this.active = true;

        // No longer required with the new websocket library
        // this.setup();

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
                Packet[] responseSequence = packetRouter.handlePacketSequence(server, this, packetSequence);
                if (responseSequence == null)
                    continue;

                // Send the response sequence to the client through the output stream
                for (Packet packet : responseSequence) {
                    if (packet.type != Packet.TYPE_PING)
                        log("Writing packet: \n" + packet + " -> " + packet.getData().toJSONString());
                    else
                        log("Pinging client.");
                    writePacket(packet);
                }
            }
        } catch (Exception e) {
            log("\033[91;3m" + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m");
        }

        log("Closing thread.");
        close();
        active = false;
        interrupt();
    }

    public void deAuthenticate() {
        if (authenticated) {
            List<ClientHandler> handlerList = server.userToHandler.get((int) user.get("id"));

            for (int i = 0; i < handlerList.size(); i++) {
                // Presumably this comparison should determine if the handlers in question are
                // the same handlers.
                // I'm not sure why simply comparing the references doesn't work, but I will
                // give this a go and
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
                                        user_notif_data)));
                }
            } catch (SQLException e) {
                log("\033[91;3mSQL error! " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        super.close();

        deAuthenticate();
    }
}
