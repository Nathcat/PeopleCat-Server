package com.nathcat.peoplecat_server.handlers;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class InitUserKey implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        // Verify the request format is correct
        JSONObject request = packets[0].getData();

        if (!request.containsKey("newPublicKey") || !request.containsKey("newPrivateKey")) {
            return new Packet[] { Packet.createError("Invalid Format",
                    "There are missing required fields from the payload!") };
        }

        //
        // Phase 1 - Re-initialise the user's key set
        //

        try {
            KeyManager.initUserKey((int) handler.user.get("id"), (JSONObject) request.get("newPublicKey"),
                    (String) request.get("newPrivateKey"));

        } catch (IOException e) {
            handler.log("\033[91m;3mKey Init Error (Phase 1): " + e.getMessage() + "\033[0m");
            return new Packet[] { Packet.createError("Key Init Error", e.getMessage()) };
        }

        //
        // Phase 2 - Update SQL of changes
        //

        try {
            PreparedStatement stmt = server.db
                    .getPreparedStatement("DELETE FROM ChatMemberships WHERE user = ?");
            stmt.setInt(1, (int) handler.user.get("id"));
            stmt.executeUpdate();
            stmt.close();

        } catch (SQLException e) {
            handler.log("\033[91m;3mKey Init Error (Phase 2): " + e.getMessage() + "\033[0m");
            return new Packet[] { Packet.createError("Key Init Error", e.getMessage()) };
        }

        return new Packet[] { Packet.createPacket(Packet.TYPE_INIT_USER_KEY, true, null) };
    }

}
