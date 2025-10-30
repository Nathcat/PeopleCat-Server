package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.simple.JSONObject;

import com.nathcat.AuthCat.AuthCat;
import com.nathcat.AuthCat.AuthResult;
import com.nathcat.peoplecat_database.Database;
import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ClientHandler;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class Authenticate implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        // Check that there is only one packet in the request
        if (packets.length > 1) {
            return new Packet[] { Packet.createError("Invalid data type",
                    "Auth request does not accept multi-packet arrays.") };
        }

        // Get the data from the single packet
        JSONObject user = packets[0].getData();

        // Send the request to AuthCat
        AuthResult authCatResponse = AuthCat.tryAuthenticate(user);
        handler.log("Got response from AuthCat: " + authCatResponse);

        // If the response is a failed authentication, respond with an error packet
        if (!authCatResponse.result) {
            return new Packet[] {
                    Packet.createError("Auth Failed", "Failed to authenticate with the given information.") };
        }

        ClientHandler ch = (ClientHandler) handler;

        // Authentication successful
        // Check if the handler was previously authenticated, if so clean up!
        ch.deAuthenticate();

        // Set relevant handler fields
        handler.authenticated = true;
        handler.user = authCatResponse.user;
        handler.user.put("id", Math.toIntExact((long) handler.user.get("id")));

        // Add this user to the handler list
        List<ClientHandler> handlerList = server.userToHandler.get((int) handler.user.get("id"));
        if (handlerList != null)
            handlerList.add(ch);
        else {
            handlerList = Collections.synchronizedList(new LinkedList<>());
            handlerList.add(ch);
            server.userToHandler.put((int) handler.user.get("id"), handlerList);
        }

        // Notify this user's online followers that they are online
        try {
            PreparedStatement stmt = server.db
                    .getPreparedStatement("SELECT follower FROM Friends WHERE id = ?");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.execute();
            JSONObject[] r = Database.extractResultSet(stmt.getResultSet());
            JSONObject user_notif_data = new JSONObject();
            user_notif_data.putAll(handler.user);
            user_notif_data.remove("password");
            user_notif_data.remove("verified");
            user_notif_data.remove("email");

            for (JSONObject jsonObject : r) {
                List<ClientHandler> followerList = server.userToHandler.get((int) jsonObject.get("follower"));

                if (followerList != null)
                    followerList.forEach((ClientHandler h) -> h.writePacket(
                            Packet.createPacket(
                                    Packet.TYPE_NOTIFICATION_USER_ONLINE,
                                    true,
                                    user_notif_data)));
            }
        } catch (SQLException e) {
            handler.log("\033[91;3mSQL error! " + e.getMessage() + "\033[0m");
        }

        // Get this user's keypair from the key manager
        JSONObject keyPair;
        try {
            keyPair = KeyManager.getUserKey((int) handler.user.get("id"));
        } catch (IOException e) {
            handler.log("\033[91;3mIO Error: " + e.getClass().getName() + "\033[0m");
            return new Packet[] { Packet.createError("Key Retrieval Error",
                    e.getClass().getName() + " occurred while trying to get the requested key.") };
        } catch (IllegalStateException e) {
            keyPair = null;
        }

        // Prepare response data and reply to the client
        JSONObject response = new JSONObject();
        response.putAll(handler.user);
        response.put("keyPair", keyPair);

        return new Packet[] { Packet.createPacket(
                Packet.TYPE_AUTHENTICATE,
                true,
                response) };
    }

}
