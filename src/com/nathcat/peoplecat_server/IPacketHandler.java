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
     * @deprecated
     * @see com.nathcat.peoplecat_server.Packet#TYPE_CREATE_NEW_USER
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
    Packet[] notificationMessage(ConnectionHandler handler, Packet[] packets);

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
     * @deprecated
     * @see Packet#TYPE_CHANGE_PFP_PATH
     */
    Packet[] changeProfilePicture(ConnectionHandler handler, Packet[] packets);

    /**
     * Get the number of currently active users
     * @param handler The handler handling the connection to the client
     * @param packets The packet sequence
     * @return The packet sequence to respond with
     * @see Packet#TYPE_GET_ACTIVE_USER_COUNT
     */
    Packet[] getActiveUserCount(ConnectionHandler handler, Packet[] packets);

    /**
     * Handle a notification about a user coming online
     * @param handler The handler handling the connection
     * @param packets The packet sequence from the endpoint
     * @return The packet sequence to reply with
     * @see Packet#TYPE_NOTIFICATION_USER_ONLINE
     */
    Packet[] notificationUserOnline(ConnectionHandler handler, Packet[] packets);

    /**
     * Handle a notification about a user going offline
     * @param handler The handler handling the connection
     * @param packets The packet sequence from the endpoint
     * @return The packet sequence to reply with
     * @see Packet#TYPE_NOTIFICATION_USER_OFFLINE
     */
    Packet[] notificationUserOffline(ConnectionHandler handler, Packet[] packets);

    /**
     * Get the friends of the currently authenticated user.
     * @param handler The handler handling the connection
     * @param packets The packet sequence from the endpoint
     * @return The packet sequence to reply with
     * @see Packet#TYPE_GET_FRIENDS
     */
    Packet[] getFriends(ConnectionHandler handler, Packet[] packets);

    /**
     * Handle a friend request
     * @param handler The handler handling the connection
     * @param packets The packet sequence from the endpoint
     * @return The packet sequence to reply with
     * @see Packet#TYPE_FRIEND_REQUEST
     */
    Packet[] friendRequest(ConnectionHandler handler, Packet[] packets);

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
            case Packet.TYPE_NOTIFICATION_MESSAGE -> notificationMessage(handler, packets);
            case Packet.TYPE_JOIN_CHAT -> joinChat(handler, packets);
            case Packet.TYPE_CHANGE_PFP_PATH -> changeProfilePicture(handler, packets);
            case Packet.TYPE_GET_ACTIVE_USER_COUNT -> getActiveUserCount(handler, packets);
            case Packet.TYPE_NOTIFICATION_USER_ONLINE -> notificationUserOnline(handler, packets);
            case Packet.TYPE_NOTIFICATION_USER_OFFLINE -> notificationUserOffline(handler, packets);
            case Packet.TYPE_GET_FRIENDS -> getFriends(handler, packets);
            case Packet.TYPE_FRIEND_REQUEST -> friendRequest(handler, packets);

            default -> throw new IllegalStateException("Unexpected value: " + packets[0].type);
        };
    }
}
