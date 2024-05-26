package com.nathcat.peoplecat_server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;

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
     * <h3>Description</h3>
     * <p>An empty packet</p>
     * <h3>Payload Format</h3>
     * <p>Empty payload</p>
     * <h3>Response Format</h3>
     * <p>Respond with another ping packet</p>
     */
    public static final int TYPE_PING = 1;
    /**
     * <h3>Purpose</h3>
     * <p>A packet of this type will be interpreted as an authentication request.</p>
     * <h3>Payload Format</h3>
     * <pre>
     *     {
     *         "username": String
     *         "password": String
     *     }
     * </pre>
     * <h3>Response Format</h3>
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
     */
    public static final int TYPE_AUTHENTICATE = 2;
    /**
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
     */
    public static final int TYPE_CREATE_NEW_USER = 3;
    /**
     * <h3>Purpose</h3>
     * <p>Inform the server that the client is closing the connection</p>
     * <h3>Payload format</h3>
     * <p>None</p>
     * <h3>Response format</h3>
     * <p>None</p>
     */
    public static final int TYPE_CLOSE = 4;
    /**
     * <h3>Purpose</h3>
     * <p>Request user data from the database. Passwords will be removed from the response.</p>
     * <h3>Payload format</h3>
     * <pre>
     *      {
     *          "ID": Int,
     *          "username": String,
     *          "display_name": String,
     *      }
     * </pre>
     * <p>Only one parameter needs to be specified, if multiple are given the following rules are applied to determine which one is used:</p>
     * <ol>
     *     <li>If ID is specified, search for the user with this ID.</li>
     *     <li>If ID is <i>not</i> specified, but username is specified, search for all users with a username which begins with the string given.</li>
     *     <li>If neither ID nor username is specified, then display_name <i>must</i> be specified, so search for all users with a display_name which begins with the string given.</li>
     * </ol>
     *
     * <h3>Response format</h3>
     * <p>If there is at least one user which meets the criteria, each packet in the sequence has the following format.</p>
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
     * <p>If there are no users which match, a packet with no payload is sent.</p>
     */
    public static final int TYPE_GET_USER = 5;

    /**
     * <h3>Purpose</h3>
     * <p>
     *     Request a list of messages in a given chat. If this chat is encrypted then the messages received will be in
     *     their encrypted format.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "ChatID": Int
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>Multiple packets will be sent, each containing one message in the following format, under TYPE_GET_MESSAGE_QUEUE:</p>
     * <pre>
     *     {
     *         "ChatID": Int,
     *         "Content" String,
     *         "SenderID": Int,
     *         "TimeSent": Long
     *     }
     * </pre>
     */
    public static final int TYPE_GET_MESSAGE_QUEUE = 6;

    /**
     * <h3>Purpose</h3>
     * <p>
     *     Send a message into a chat.
     * </p>
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "ChatID": Int,
     *         "Content": String,
     *         "TimeSent": Long
     *     }
     * </pre>
     * <h3>Response format</h3>
     * <p>
     *     The server will issue a ping packet in response.
     * </p>
     */
    public static final int TYPE_SEND_MESSAGE = 7;

    /**
     * <h3>Purpose</h3>
     * <p>
     *     This is to be sent from the server to the connected client applications, it tells the client that they have
     *     received a new message.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "title": String,
     *         "message": String,
     *         "ChatID": Int
     *     }
     * </pre>
     *
     * <h3>Response format</h3>
     * <p>The client should is not expected to reply to this packet.</p>
     */
    public static final int TYPE_NOTIFICATION_MESSAGE = 8;

    /**
     * <h3>Purpose</h3>
     * <p>
     *     This message indicates that a client wishes to join a chat.
     * </p>
     *
     * <h3>Payload format</h3>
     * <pre>
     *     {
     *         "ChatID": Int,
     *         "JoinCode": String,
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
     *         "ChatID": Int,
     *         "Name": String,
     *         "KeyID": Int,
     *         "JoinCode": String
     *     }
     * </pre>
     */
    public static final int TYPE_JOIN_CHAT = 9;

    /**
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
     */
    public static final int TYPE_CHANGE_PFP_PATH = 10;

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
        StringBuilder sb = new StringBuilder();
        for (byte b : payload) {
            sb.append((char) b);
        }

        try {
            return (JSONObject) new JSONParser().parse(sb.toString());

        } catch (ParseException e) {
            return null;
        }
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
        p.payload = data.toJSONString().getBytes();

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

            dos.writeInt(json.toJSONString().length());
            dos.write(json.toJSONString().getBytes());
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
            String s = payload.toJSONString();
            dos.writeInt(s.length());
            dos.write(s.getBytes());
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
