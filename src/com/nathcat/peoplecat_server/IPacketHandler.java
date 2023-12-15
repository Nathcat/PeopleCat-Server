package com.nathcat.peoplecat_server;


/**
 * Provides methods which can be implemented and used to handle packet types.
 *
 * @author Nathan Baines
 */
public interface IPacketHandler {
    /**
     * Handles error packets
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] error(ConnectionHandler handler, Packet[] packets);
    /**
     * Handles ping packets
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] ping(ConnectionHandler handler, Packet[] packets);
    /**
     * Handles authentication packets
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] authenticate(ConnectionHandler handler, Packet[] packets);
    /**
     * Handles new user packets
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] createNewUser(ConnectionHandler handler, Packet[] packets);
    /**
     * Handles close packets
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] close(ConnectionHandler handler, Packet[] packets);

    /**
     * Handle a packet sequence by determining which handler method to pass it to. Uses the type of the first packet
     * in the sequence to determine the appropriate handler method.
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return The resulting response packet sequence
     */
    default Packet[] handle(ConnectionHandler handler, Packet[] packets) {
        return switch (packets[0].type) {
            case Packet.TYPE_ERROR -> error(handler, packets);
            case Packet.TYPE_PING -> ping(handler, packets);
            case Packet.TYPE_AUTHENTICATE -> authenticate(handler, packets);
            case Packet.TYPE_CREATE_NEW_USER -> createNewUser(handler, packets);
            case Packet.TYPE_CLOSE -> close(handler, packets);

            default -> throw new IllegalStateException("Unexpected value: " + packets[0].type);
        };
    }
}
