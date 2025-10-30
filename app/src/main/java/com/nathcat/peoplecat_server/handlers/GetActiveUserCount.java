package com.nathcat.peoplecat_server.handlers;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetActiveUserCount implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject d = new JSONObject();
        d.put("usersOnline", server.handlers.size());

        return new Packet[] { Packet.createPacket(
                Packet.TYPE_GET_ACTIVE_USER_COUNT,
                true,
                d) };
    }

}
