package com.nathcat.peoplecat_server.handlers;

import com.nathcat.peoplecat_server.Packet;

/**
 * Functional interface providing a handler for a packet sequence.
 * @author Nathan Baines
 */
public interface IPacketHandler {
    /**
     * Handle a sequence of packets of the type specified by this handler
     * @param packets The packet sequence to handle
     * @return The packet sequence to reply with
     */
    public Packet[] handle(com.nathcat.peoplecat_server.Server server, com.nathcat.peoplecat_server.ConnectionHandler handler, Packet[] packets);
}
