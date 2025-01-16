package com.nathcat.peoplecat_server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * <p>Class for creating data which can be sent / received by the server / client.</p>
 *
 * @author Nathan Baines
 *
 * <h3>Specification</h3>
 * <table>
 *     <tr><th>Byte count</th><th>Data type</th><th>Purpose</th></tr>
 *     <tr><td>4</td><td>Integer</td><td>Packet type</td></tr>
 *     <tr><td>1</td><td>Boolean</td><td>If true, this is the last packet in the sequence, if false, there are more packets to come</td></tr>
 *     <tr><td>4</td><td>Integer</td><td>Payload length (in bytes)</td></tr>
 *     <tr><td>Defined by payload length</td><td>String</td><td>JSON string containing the request body</td></tr>
 * </table>
 */
public class Packet {
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Description</h3>
     * <p>Used to signify that an error has occurred, the payload provides information about this error.</p>
     * <h3>Payload Format</h3>
     * <pre>
     *     {
     *         "name": String,
     *         "msg": String
     *     }
     * </pre>
     * <h3>Response Format</h3>
     * <p>Should not reply to this packet.</p>
     */
    public static final int TYPE_ERROR = 0;
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Description</h3>
     * <p>An empty packet</p>
     * <h3>Payload Format</h3>
     * <p>Empty payload</p>
     * <h3>Response Format</h3>
     * <p>Respond with another ping packet</p>
     */
    public static final int TYPE_PING = 1;
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>A packet of this type will be interpreted as an authentication request.</p>
     * <h3>Payload Format</h3>
     * <pre>
     *     {
     *         "username": String
     *         "password": String
     *     }
     * </pre>
     * <p>
     *     Or you might wish to authenticate with an AuthCat cookie:
     * </p>
     * <pre>
     *     {
     *         "cookieAuth": String  // Pre 4.2.0 this is named cookie-auth
     *     }
     * </pre>
     * <p>
     *     Where the <code>cookieAuth</code> string is the value of the <code>AuthCat-SSO</code> cookie.
     * </p>
     * <p>
     *     Cookie auth is available from server version 4.1.0, in this version the authentication flow requires <i><b>either</b></i>
     *     cookie auth or normal auth to be specified, i.e. one or the other and not both. If the <code>cookieAuth</code>
     *     field is present, the server <i>will not attempt normal credential authentication</i>.
     * </p>
     * <p>
     *     However, from server version 4.1.1, the authentication flow is such that if <code>cookie-auth</code> is specified,
     *     the server will try cookie authentication, and upon failure will try credential authentication.
     * </p>
     * <h3>Response Format</h3>
     * <p>
     *     Note that the key pair field will be null if the user has no initialised key pair.
     * </p>
     * <pre>
     *     {
     *         "id": Int,
     *         "username": String,
     *         "fullName": String,
     *         "password": String,
     *         "pfpPath": String,
     *         "verified": Boolean,
     *         "passwordUpdated": Boolean,
     *         "keyPair": {  // Since version 5.0.0
     *             "publicKey": <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a>,
     *             "privateKey": Hex string representing an encrypted <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a>
     *         }
     *     }
     * </pre>
     */
    public static final int TYPE_AUTHENTICATE = 2;
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>Creates a new user with the data specified in the payload.</p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "username": String,
     *         "password": String,
     *         "display_name", String
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <pre>
     *     {
     *         "ID": Int,
     *         "username": String,
     *         "display_name": String,
     *         "password": String,
     *         "time_created": Long,
     *         "pfp_path": String,
     *         "cover_pic_path": String
     *     }
     * </pre>
     * @deprecated This will be delegated to AuthCat and no longer handled by PeopleCat, Server will reject packets
     *             of this type.
     */
    public static final int TYPE_CREATE_NEW_USER = 3;
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>Inform the server that the client is closing the connection</p>
     * <h3>Payload format</h3>
     * <p>None</p>
     * <h3>Response format</h3>
     * <p>None</p>
     */
    public static final int TYPE_CLOSE = 4;
    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>Request user data from the database. Passwords will be removed from the response.</p>
     * <p>
     *     <b><i>With the AuthCat integration, it is recommended that clients go straight to AuthCat for user
     *     searches. Documentation for this process can be found <a href="https://github.com/Nathcat/data.nathcat.net/blob/main/sso/docs/AuthCat.md">here</a></i></b>
     * </p>
     * <h3>Payload format</h3>
     * <p>
     *     The data required by this request is the same as is required by AuthCat's user search feature, i.e. you may
     *     request a user's public data by providing one of the following payloads.
     * </p>
     * <h4>Request by user ID</h4>
     * <pre>
     *     {
     *         "id": Integer
     *     }
     * </pre>
     * <h4>Request by username search</h4>
     * <pre>
     *     {
     *         "username": String
     *     }
     * </pre>
     * <p>
     *     Note that AuthCat appends a "%" to the end of the provided username, so it will return all users which have
     *     a username <i>which starts with</i> the provided username.
     * </p>
     * <h4>Request by full name search</h4>
     * <pre>
     *     {
     *         "fullName": String
     *     }
     * </pre>
     * <p>
     *     Note that AuthCat appends a "%" to the end of the provided name, so it will return all users which have
     *     a name <i>which starts with</i> the provided name.
     * </p>
     * <h4>Combining searches</h4>
     * <p>
     *     You may search for <i>both</i> a username and full name at the same time, this will return the same results
     *     as if you did them individually, but if the <code>id</code> field is specified, AuthCat will <i>only search
     *     by the provided <code>id</code></i>, and will ignore any other provided information.
     * </p>
     */
    public static final int TYPE_GET_USER = 5;

    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Request a list of messages in a given chat. If this chat is encrypted then the messages received will be in
     *     their encrypted format.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "chatId": Int  // Pre 4.2.0 this is named ChatID
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>
     *     The server will respond with a sequence of packets, each containing the data for one message in the chat.
     *     However before this, from version 4.1.2, one packet will be sent containing the following payload:
     * </p>
     * <pre>
     *     {
     *         "messageCount": Integer  // Pre 4.2.0 this is named message-count
     *     }
     * </pre>
     * <p>
     *     Which will give the number of messages in the chat. This, and all following packets will be sent under the
     *     type TYPE_GET_MESSAGE_QUEUE. Server versions prior to 4.1.2 will not send the first packet and just send the
     *     packet sequence.
     * </p>
     * <p>
     *     The format of all packets following the above will be:
     * </p>
     * <pre>
     *     {
     *         "chatId": Int,       // Pre 4.2.0 this is named ChatID
     *         "content" String,    // Pre 4.2.0 this is named Content
     *         "senderId": Int,     // Pre 4.2.0 this is named SenderID
     *         "timeSent": Long     // Pre 4.2.0 this is named TimeSent
     *     }
     * </pre>
     * <p>
     *     With the last packet specifying <code>isFinal = true</code>.
     * </p>
     */
    public static final int TYPE_GET_MESSAGE_QUEUE = 6;

    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Send a message into a chat.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "chatId": Int,       // Pre 4.2.0 this is named ChatID
     *         "content": String,   // Pre 4.2.0 this is named Content
     *         "timeSent": Long     // Pre 4.2.0 this is named TimeSent
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>
     *     The server will issue a ping packet in response.
     * </p>
     */
    public static final int TYPE_SEND_MESSAGE = 7;

    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     This is to be sent from the server to the connected client applications, it tells the client that they have
     *     received a new message.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "title": String,         // Not included since 4.2.0
     *         "message": String,       // Not included since 4.2.0
     *         "chatId": Int,           // Pre 4.2.0 this is named ChatID
     *         // "Message" field only included since server v4.1.3
     *         "message": {             // Pre 4.2.0 this is named Message
     *              "chatId": Int,      // Pre 4.2.0 this is named ChatID
     *              "content" String,   // Pre 4.2.0 this is named Content
     *              "senderId": Int,    // Pre 4.2.0 this is named SenderID
     *              "timeSent": Long    // Pre 4.2.0 this is named TimeSent
     *         }
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>The client should is not expected to reply to this packet.</p>
     */
    public static final int TYPE_NOTIFICATION_MESSAGE = 8;

    /**
     * <p>Since version 1.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     This message indicates that a client wishes to join a chat.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "chatId": Int,       // Pre 4.2.0 this is named ChatID
     *         "joinCode": String,  // Pre 4.2.0 this is named JoinCode, not required from 5.0.0
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     The server will reply under the same packet type to indicate success, with the full information about the chat,
     *     or will reply with an error packet to indicate failure.
     * </p>
     * <pre>
     *     {
     *         "chatId": Int,               // Pre 4.2.0 this is named ChatID
     *         "name": String,              // Pre 4.2.0 this is named Name
     *         "keyId": Int,                // Pre 4.2.0 this is named KeyID
     *         "joinCode": String,          // Pre 4.2.0 this is named JoinCode, not present from 5.0.0
     *         "icon": String,              // Present from 5.0.0
     *     }
     * </pre>
     */
    public static final int TYPE_JOIN_CHAT = 9;

    /**
     * <p>Since version 1.1.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     This indicates a request to change the profile picture of the currently authenticated user.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "NewPath": String
     *     }
     * </pre>
     * <p>
     *     Note that "NewPath" should be either a <i>full</i> url to the profile picture image, or the string "null",
     *     indicating the default profile picture.
     * </p>
     *
     * <h3>Response format</h3>
     * <p>
     *     The server will either respond with a packet with an empty payload under the type TYPE_CHANGE_PFP_PATH
     *     to indicate success, or it will reply with an error packet to indicate failure.
     * </p>
     * @deprecated This will be delegated to AuthCat and no longer handled by PeopleCat. The server will reject packets
     *             of this type.
     */
    public static final int TYPE_CHANGE_PFP_PATH = 10;

    /**
     * <p>Since version 2.1.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Get the number of users currently online
     * </p>
     *
     * <h3>Payload format</h3>
     * <p>
     *     No payload required
     * </p>
     *
     * <h3>Response format</h3>
     * <p>
     *     {
     *         "usersOnline": Int   // Pre 4.2.0 this is named users-online
     *     }
     * </p>
     */
    public static final int TYPE_GET_ACTIVE_USER_COUNT = 11;

    /**
     * <p>Since version 4.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Alerts the receiver that the specified user has just come online.
     * </p>
     * <p>
     *     Note that this should only ever be sent from the server to the client,
     *     the same as other notification packets.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "id": Integer,
     *         "username": String,
     *         "fullName": String,
     *         "pfpPath": String
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     Should not respond to this packet.
     * </p>
     */
    public static final int TYPE_NOTIFICATION_USER_ONLINE = 12;

    /**
     * <p>Since version 4.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Alerts the receiver that the specified user has just gone offline.
     * </p>
     * <p>
     *     Note that this should only ever be sent from the server to the client,
     *     the same as other notification packets.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "id": Integer,
     *         "username": String,
     *         "fullName": String,
     *         "pfpPath": String
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     Should not respond to this packet.
     * </p>
     */
    public static final int TYPE_NOTIFICATION_USER_OFFLINE = 13;

    /**
     * <p>Since version 4.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Get a list of the currently authenticated user's friends.
     * </p>
     *
     * <h3>Payload format</h3>
     * <p>
     *     No payload is required for this request.
     * </p>
     *
     * <h3>Response format</h3>
     * <p>
     *     Server will reply with a sequence of packets specifying the basic information of the authenticated user's
     *     friends, each packet will contain one user.
     * </p>
     * <p>
     *     This is the same as the get user request response format, but will be under the get friends packet type.
     * </p>
     */
    public static final int TYPE_GET_FRIENDS = 14;

    /**
     * <p>Since version 4.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Manipulate a user's friend requests. This includes actions such as sending, and accepting / declining
     * </p>
     * <p>
     *     Note that the server will send a packet to the client under this type when a friend request is sent to the
     *     user that client is logged in as, such a packet will contain the following payload format detailing the
     *     information of the user that sent the request.
     * </p>
     * <pre>
     *     {
     *         "id": Integer
     *         "username": String
     *         "fullName": String
     *         "pfpPath": String
     *     }
     * </pre>
     *
     * <h3>Payload format</h3>
     * <p>
     *     This packet type has a conditional payload, but must always contain the <code>"action"</code> field:
     * </p>
     * <pre>
     *     {
     *         "action": String -> "SEND", "ACCEPT", "DECLINE",
     *         <code>IF "action" = "SEND" THEN:</code>
     *         "recipient": Integer,
     *         <code>OR IF "action" = "ACCEPT" OR "DECLINE" THEN:</code>
     *         "id": Integer
     *         <code>OR IF "action" = "GET" THEN NONE REQUIRED</code>
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     If <code>"action" = "SEND"</code> in the request, then the response is of this format:
     * </p>
     * <pre>
     *     {
     *         "id": Integer
     *     }
     * </pre>
     * <p>
     *     If <code>"action" = "GET"</code>, then a sequence of packets is supplied, each with the payload:
     * </p>
     * <pre>
     *     {
     *         "id": Integer,
     *         "sender": Integer
     *     }
     * </pre>
     * <p>
     *     Otherwise, there is no response beside error messages.
     * </p>
     */
    public static final int TYPE_FRIEND_REQUEST = 15;

    /**
     * <p>Since version 4.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Get information about the current version of the server
     * </p>
     *
     * <h3>Payload format</h3>
     * <p>No payload required</p>
     *
     * <h3>Response format</h3>
     * <pre>
     *     {
     *         "version": String,
     *         "serverTime": String,            // Pre 4.2.0 this is named server-time
     *         "pushServicePublicKey": String   // Present from 5.0.0
     *     }
     * </pre>
     */
    public static final int TYPE_GET_SERVER_INFO = 16;

    /**
     * <p>Since version 5.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Get information about which chats the authenticated user is a member of
     * </p>
     *
     * <h3>Payload format</h3>
     * <p>No payload required</p>
     *
     * <h3>Response format</h3>
     * <p>
     *     If the user is not a member of any chats, the server will respond with an error packet with the following payload:
     * </p>
     * <pre>
     *     {
     *         "name": "No Chat Memberships",
     *         "msg": "This user is not a member of any chats."
     *     }
     * </pre>
     * <p>
     *     Note that other general errors that may occur during this request will also be served under this packet type, but with a different payload,
     *     so ensure that your client does not assume an error response from this request means there are no chat memberships, as that may not be the case.
     * </p>
     * <p>
     *     Otherwise, the server will respond with a packet stream, each packet containing information for a single chat,
     *     in the following format:
     * </p>
     *
     * <pre>
     *     {
     *         "chatId": Integer,
     *         "name": String,
     *         "keyId": Integer,
     *         "joinCode": String,
     *         "icon": String,
     *         "key": Hex string containing a <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> encrypted with the user key
     *     }
     * </pre>
     */
    public static final int TYPE_GET_CHAT_MEMBERSHIPS = 17;
    /**
     * <p>Since version 5.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Allows an authenticated client to create a new chat
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "name": String,
     *         "icon": String,                                                          // This field is optional!
     *         "key": Hex string of a JSON Web Key encrypted with the user key          // This field is only required for encrypted chats, leave unset to create a public chat.
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     In the event that the server fails to create the chat it will respond with and error packet in the following format:
     * </p>
     * <pre>
     *     {
     *         "name": "Failed To Create Chat",
     *         "msg": String
     *     }
     * </pre>
     *
     * <p>
     *     If the server successfully creates the chat, it will reply with the data generated for the chat by the server, in the following format:
     * </p>
     * <pre>
     *     {
     *         "chatId": Integer,
     *         "name": String,                                                          // This will be the same as the "name" field specified in the request payload
     *         "keyId": Integer,
     *         "icon": String,                                                          // This will be the same as the "icon" field specified in the request payload
     *         "key": Hex string of a JSON Web Key encrypted with the user key          // This will be the same as the "privateKey" field specified in the request payload
     *     }
     * </pre>
     * <p>
     *     Note that the authenticated user will automatically be added as a member of the created chat once created
     * </p>
     */
    public static final int TYPE_CREATE_CHAT = 18;
    /**
     * <p>Since version 5.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     This packet type allows a client to initialise the public key of the currently authenticated user.
     * </p>
     * <p>
     *     Note that all encrypted data will be deleted upon this request's success. Meaning the user will no longer have
     *     access to their old chats.
     * </p>
     * <p>
     *     The user's asymmetric keypair should be generated on the client-side. The public key should be sent to the user
     *     in an unencrypted JSON Web Key format, while the private key should be encrypted using a symmetric key derived
     *     from the user's plaintext password using the <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/deriveKey#pbkdf2">PBKDF2 algorithm</a>,
     *     and should be a <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/encrypt#aes-gcm">AES-GCM</a> key.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "newPublicKey": <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a>,
     *         "newPrivateKey": Hex string representing a <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> encrypted with the user's symmetric key derived from their plaintext password
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>
     *     If successful, the server will respond with a packet of the same type, with an empty payload. Otherwise it
     *     will respond with an error packet detailing the error.
     * </p>
     */
    public static final int TYPE_INIT_USER_KEY = 19;

    /**
     * <p>Since version 5.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Get the public key of the requested user.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "id": Integer
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>
     *     The server will either respond with a packet under this type, with the payload containing the requested
     *     user's public key in <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> format,
     *     or will respond with an error packet if no such key exists.
     * </p>
     */
    public static final int TYPE_GET_USER_KEY = 20;

    /**
     * <p>Since version 5.0.0</p>
     * <h3>Purpose</h3>
     * <p>
     *     Allows a user to add a friend to a chat. The primary use of this is when adding new users
     *     to encrypted chats, public chats should be joined using the join chat request.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "id": Integer,       // ID of user to add to the chat
     *         "chatId": Integer,   // ID of the chat to add the user to
     *         "key": Hex string of a <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> encrypted with the <i>other</i> user's public key
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>
     *     The server will either reply with an error packet upon failiure, or a packet under the same type with an empty
     *     payload upon success.
     * </p>
     */
    public static final int TYPE_ADD_TO_CHAT = 21;


    /**
     * The type of request specified by the packet
     */
    public int type;
    /**
     * Whether this packet is the last in its sequence or not.
     */
    public boolean isFinal;
    /**
     * The raw data in the payload, this is either a JSON object or an array of JSON objects.
     */
    public byte[] payload;

    public Packet() {}

    public Packet(InputStream inStream) throws IOException {
        DataInputStream input = new DataInputStream(inStream);
        type = input.readInt();
        isFinal = input.readBoolean();
        int length = input.readInt();
        payload = new byte[length];

        try {
            int res = inStream.read(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Transform the given data into a JSONObject
     * @return The JSONObject contained within this packet's payload
     */
    public JSONObject getData() {
        String p = new String(payload, StandardCharsets.UTF_8);

        try {
            return (JSONObject) new JSONParser().parse(p);

        } catch (ParseException e) {
            System.out.println("\033[91;3m----- PACKET JSON DECODE ERROR -----\n");
            System.out.println("Culprit String: " + p + "\n\nError:\n");
            e.printStackTrace();
            System.out.println("\033[0m");
            return null;
        }
    }

    /**
     * Get the data contained by this packet in JSON websocket format.
     * @return A <code>JSONObject</code> containing the JSON websocket format data contained by this packet.
     */
    public JSONObject getData_WebSocket() {
        JSONObject d = getData();
        d = d == null ? new JSONObject() : d;
        d.put("type", type);
        d.put("isFinal", isFinal);
        return d;
    }

    /**
     * Create a packet from a JSONObject, used in websocket communication.
     * @param data The JSON data from the websocket
     * @return The equivalent packet
     */
    public static Packet fromData(JSONObject data) {
        Packet p = new Packet();
        p.type = ((Long) data.get("type")).intValue();
        p.isFinal = (boolean) data.get("isFinal");
        data.remove("type");
        data.remove("isFinal");
        p.payload = data.toJSONString().getBytes(StandardCharsets.UTF_8);

        return p;
    }

    /**
     * Get the byte representation of the packet
     * @return A byte array containing the data required to correctly reconstruct the packet when it is received
     */
    public byte[] getBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeInt(type);
            dos.writeBoolean(isFinal);
            dos.writeInt(payload.length);
            dos.write(payload);

            byte[] res = baos.toByteArray();
            dos.close();
            baos.close();

            return res;

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a simple, empty ping packet
     * @return Packet with type TYPE_PING, and no payload
     */
    public static Packet createPing() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(TYPE_PING);
            dos.writeBoolean(true);
            dos.writeInt(0);
            dos.flush();

            byte[] payload = baos.toByteArray();
            dos.close();
            baos.close();

            return new Packet(new ByteArrayInputStream(payload));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create an error packet
     * @param msg The message describing the error
     * @return Packet with type TYPE_ERROR
     */
    public static Packet createError(String name, String msg) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(TYPE_ERROR);
            dos.writeBoolean(true);

            JSONObject json = new JSONObject();
            json.put("name", name);
            json.put("msg", msg);

            byte[] s = json.toJSONString().getBytes(StandardCharsets.UTF_8);

            dos.writeInt(s.length);
            dos.write(s);
            dos.flush();

            byte[] payload = baos.toByteArray();
            dos.close();
            baos.close();

            return new Packet(new ByteArrayInputStream(payload));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a basic data packet
     * @param type The type of the packet
     * @param isFinal Whether this is the final packet in its sequence or not
     * @param payload The data to be entered into this packet's payload
     * @return The resulting packet object.
     */
    public static Packet createPacket(int type, boolean isFinal, JSONObject payload) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);

        try {
            dos.writeInt(type);
            dos.writeBoolean(isFinal);
            byte[] s = payload != null ? payload.toJSONString().getBytes(StandardCharsets.UTF_8) : new byte[0];
            dos.writeInt(s.length);
            dos.write(s);
            dos.flush();

            byte[] result = baos.toByteArray();
            dos.close();
            baos.close();

            return new Packet(new ByteArrayInputStream(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Create a packet which informs the server / client of a connection being closed.
     * @return The closing packet
     */
    public static Packet createClose() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(TYPE_CLOSE);
            dos.writeBoolean(true);
            dos.writeInt(0);
            dos.flush();

            byte[] payload = baos.toByteArray();
            dos.close();
            baos.close();

            return new Packet(new ByteArrayInputStream(payload));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "{\n\t\"type\": " + type + ",\n\t\"isFinal\": " + isFinal + ",\n\t\"length\": " + payload.length + "\n}";
    }
}
