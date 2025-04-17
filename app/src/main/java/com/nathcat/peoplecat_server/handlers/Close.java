package com.nathcat.peoplecat_server.handlers;

import com.nathcat.peoplecat_server.Server;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;

public class Close implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        handler.close();
        return null;
    }
    
}
