package com.nathcat.peoplecat_server.handlers;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class Error implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        JSONObject d = packets[0].getData();

        handler.log("\033[91;3mError from client:\n" + d.get("name") + ":\n" + d.get("msg") + "\033[0m");
        return new Packet[0];
    }

}
