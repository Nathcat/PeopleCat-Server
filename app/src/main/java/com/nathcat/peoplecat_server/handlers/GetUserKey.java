package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetUserKey implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        // Check the request contains the required field
        JSONObject request = packets[0].getData();
        if (!request.containsKey("id")) {
            return new Packet[] {
                    Packet.createError("Invalid Format", "You must specify the id of the user!") };
        }

        // Get the field from the request data, and attempt to retrieve the user's key.
        int id = request.get("id").getClass() == Long.class ? Math.toIntExact((long) request.get("id"))
                : (int) request.get("id");
        JSONObject key;
        try {
            key = KeyManager.getUserKey(id);
        } catch (IOException e) {
            handler.log("\033[91;3mIO Error: " + e.getClass().getName() + "\033[0m");
            return new Packet[] { Packet.createError("Key Retrieval Error",
                    e.getClass().getName() + " occurred while trying to get the requested key.") };
        } catch (IllegalStateException e) {
            return new Packet[] { Packet.createError("Key Not Found",
                    "The user has a key set, but the key set does not contain a user key! Try re-initialising the user's key.") };
        }

        if (key == null) {
            return new Packet[] { Packet.createError("Key Set Not Found",
                    "No key set can be found for the specified user.") };
        }

        return new Packet[] {
                Packet.createPacket(
                        Packet.TYPE_GET_USER_KEY,
                        true,
                        (JSONObject) key.get("publicKey"))
        };
    }

}
