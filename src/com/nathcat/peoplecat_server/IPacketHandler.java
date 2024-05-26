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
     * Get a user from the database
     * @param handler The ConnectionHandler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] getUser(ConnectionHandler handler, Packet[] packets);

    /**
     * Get the messages from a chat
     * @param handler The connection handler handling the connection
     * @param packets The packet sequence
     * @return Response packet sequence
     */
    Packet[] getMessageQueue(ConnectionHandler handler, Packet[] packets);

    /**
     * Send a message into a chat
     * @param handler The connection handler handling the connection
     * @param packets The packet sequence
     * @return The response packet sequence
     */
    Packet[] sendMessage(ConnectionHandler handler, Packet[] packets);

    /**
     * Handle a message notification
     * @param handler The connection handler handing the connection
     * @param packets The packet sequence
     * @return The response packet sequence
     */
    Packet[] notifitcationMessage(ConnectionHandler handler, Packet[] packets);

    /**
     * Join a chat
     * @param handler The connection handler handling the connection
     * @param packets The packet sequence
     * @return The response packet sequence
     */
    Packet[] joinChat(ConnectionHandler handler, Packet[] packets);

    /**
     * Change the authenticated user's profile picture
     * @param handler The connection handler handling the connection
     * @param packets The packet sequence
     * @return The response packet sequence
     */
    Packet[] changeProfilePicture(ConnectionHandler handler, Packet[] packets);

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
            case Packet.TYPE_GET_USER -> getUser(handler, packets);
            case Packet.TYPE_GET_MESSAGE_QUEUE -> getMessageQueue(handler, packets);
            case Packet.TYPE_SEND_MESSAGE -> sendMessage(handler, packets);
            case Packet.TYPE_NOTIFICATION_MESSAGE -> notifitcationMessage(handler, packets);
            case Packet.TYPE_JOIN_CHAT -> joinChat(handler, packets);
            case Packet.TYPE_CHANGE_PFP_PATH -> changeProfilePicture(handler, packets);

            default -> throw new IllegalStateException("Unexpected value: " + packets[0].type);
        };
    }
}
