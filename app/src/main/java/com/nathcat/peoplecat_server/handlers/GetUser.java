package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import com.nathcat.AuthCat.AuthCat;
import com.nathcat.AuthCat.Exceptions.InvalidResponse;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetUser implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get user request does not accept multi-packet arrays.") };

        // Get the request data
        JSONObject request = packets[0].getData();
        JSONObject[] users;

        JSONObject response;
        try {
            response = AuthCat.userSearch(request);
        } catch (InvalidResponse | IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (((String) response.get("status")).contentEquals("success")) {
            ArrayList<JSONObject> u = new ArrayList<>();
            JSONObject results = (JSONObject) response.get("results");
            for (Object k : results.keySet()) {
                u.add((JSONObject) results.get(k));
            }

            users = u.toArray(new JSONObject[0]);

            // In case no users are returned
            if (users.length == 0) {
                return new Packet[] {
                        Packet.createPacket(Packet.TYPE_GET_USER, true, new JSONObject())
                };
            }
        } else {
            return new Packet[] { Packet.createError("AuthCat error", (String) response.get("message")) };
        }

        // Create the response packet sequence
        Packet[] reply = new Packet[users.length];
        for (int i = 0; i < users.length - 1; i++) {
            users[i].remove("Password");
            reply[i] = Packet.createPacket(
                    Packet.TYPE_GET_USER,
                    false,
                    users[i]);
        }

        users[users.length - 1].remove("Password");
        reply[reply.length - 1] = Packet.createPacket(
                Packet.TYPE_GET_USER,
                true,
                users[users.length - 1]);

        return reply;
    }

}
