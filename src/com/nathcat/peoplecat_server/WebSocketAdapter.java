package com.nathcat.peoplecat_server;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a utility class which allows the server to handle websocket connections for web based implementations
 * of the PeopleCat platform.
 *
 * @author Nathan Baines
 */
public class WebSocketAdapter {
    /**
     * Websocket message fragment class. Follows the RFC 6455 specification:
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455">RFC 6455</a>
     */
    public static class Fragment {
        /**
         * Is this the final fragment in the message?
         */
        public final boolean finalFragment;
        public final boolean RSV1;
        public final boolean RSV2;
        public final boolean RSV3;
        /**
         * The type of data sent
         */
        public final int opcode;
        /**
         * The payload buffer
         */
        public final byte[] payload;
        public byte[] key;

        /**
         * Interpret a message buffer.
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.2">RFC 6455 - Base Framing Protocol</a>
         * @param stream A byte stream containing the buffer.
         * @throws IOException
         */
        public Fragment(InputStream stream) throws IOException {
            byte header = stream.readNBytes(1)[0];

            finalFragment = (header & 0x80) == 0x80;
            RSV1 = (header & 0x40) == 0x40;
            RSV2 = (header & 0x20) == 0x20;
            RSV3 = (header & 0x10) == 0x10;
            opcode = (header & 0xF);

            byte payload_len = stream.readNBytes(1)[0];
            int length_descriptor = payload_len & 0x7F;
            int[] length = new int[1];
            boolean payloadMasked = (payload_len & 0x80) == 0x80;

            if (length_descriptor >= 0 && length_descriptor <= 125) {
                length = new int[] {length_descriptor};
            }
            else if (length_descriptor == 126) {
                length[0] |= stream.readNBytes(1)[0];
                length[0] |= ((int) stream.readNBytes(1)[0] << 8);
            }
            else if (length_descriptor == 127){
                for (int i = 0; i < 8; i++) {
                    length[i / 4] |= ((int) stream.readNBytes(1)[0] << (i * 8));
                }
            }
            else {
                throw new RuntimeException("Invalid length descriptor: " + length_descriptor);
            }

            if (payloadMasked) {
                key = stream.readNBytes(4);
                payload = DecodeBuffer(stream, key);
            }
            else {
                payload = stream.readAllBytes();
            }
        }

        /**
         * Decode the masked payload buffer
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.3">RFC 6455 - Client-To-Server Masking</a>
         * @param stream The stream, should include both the key and the masked payload
         * @return Decoded payload buffer
         * @throws IOException
         */
        private static byte[] DecodeBuffer(InputStream stream, byte[] key) throws IOException {
            byte[] buffer = stream.readAllBytes();
            for (int i = 0; i < buffer.length; i++) {
                buffer[i] = (byte) (buffer[i] ^ key[i % 4]);
            }

            return buffer;
        }

        /**
         * Use the opcode to interpret the payload buffer into a Java object which can be used in a program.
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.2">RFC 6455 - Base Framing Protocol</a>
         * @return Java interpretation of the payload buffer
         */
        public Object GetData() {
            if (opcode == 1) {
                char[] c_buffer = new char[payload.length];
                for (int i = 0; i < payload.length; i++) {
                    c_buffer[i] = (char) payload[i];
                }
                return String.valueOf(c_buffer);
            }

            return null;
        }

        /**
         * Create a buffer of string data to send to the client, within a websocket fragment
         * @param data The string to send to the client
         * @param key The key to encode the data
         * @return The buffer to send to the client
         */
        public static byte[] FromData(String data, byte[] key) {
            byte header = (byte) 0x81;

            char[] c_buffer = data.toCharArray();
            byte[] payload = new byte[c_buffer.length];
            for (int i = 0; i < c_buffer.length; i++) {
                payload[i] = (byte) (c_buffer[i] ^ key[i % 4]);
            }

            byte length_desc = 0;
            byte[] extra_length = new byte[0];
            if (payload.length >= 0 && payload.length <= 125) {
                length_desc = (byte) payload.length;
            } else if (payload.length <= 0xFFFF) {
                length_desc = 126;
                extra_length = new byte[] {(byte) (payload.length & 0xFF), (byte) (payload.length & 0xFF00)};
            }
            else {
                length_desc = 127;
                extra_length = new byte[8];
                for (int i = 0; i < 8; i++) {
                    extra_length[i] = (byte) (payload.length & (0xFF << (i * 8)));
                }
            }

            length_desc |= 0x80;

            byte[] buffer = new byte[6 + extra_length.length + payload.length];
            buffer[0] = header;
            buffer[1] = length_desc;
            System.arraycopy(extra_length, 0, buffer, 2, extra_length.length);
            System.arraycopy(key, 0, buffer, 2 + extra_length.length, 4);
            System.arraycopy(payload, 0, buffer, 6 + extra_length.length, payload.length);

            return buffer;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{\n");
            for (Field field : this.getClass().getFields()) {
                try {
                    sb.append("\t" + field.getName() + ": " + (field.getType() == byte[].class ? Arrays.toString((byte[]) field.get(this)) : field.get(this)) + "\n");
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            sb.append("}");

            return sb.toString();
        }
    }

    /**
     * Detect whether a TCP socket has a web socket client or not.
     * @param socket The TCP socket
     * @return True if the client is a websocket, false otherwise.
     * @throws IOException Thrown when an I/O operation fails
     */
    public static boolean detectWebSocket(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        Scanner s = new Scanner(in, "UTF-8");
        String data = s.useDelimiter("\\r\\n\\r\\n").next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        return get.find();
    }

    /**
     * Upgrade a TCP socket to a web socket.
     * @param socket The socket to upgrade
     * @throws IOException Thrown when an I/O operation fails
     */
    public static void upgradeSocket(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();

        Scanner s = new Scanner(in, "UTF-8");
        String data = s.useDelimiter("\\r\\n\\r\\n").next();

        Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
        match.find();
        byte[] response;
        try {
            response = ("HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes("UTF-8")))
                    + "\r\n\r\n").getBytes("UTF-8");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        out.write(response, 0, response.length);
    }

    /**
     * Transform a packet into a websocket fragment message buffer.
     * @param packet The packet to transform
     * @return A byte array containing the fragment data to be sent through the websocket.
     */
    public static byte[] createMessageBuffer(Packet packet) {
        JSONObject packetData = packet.getData();
        packetData.put("type", packet.type);
        packetData.put("isFinal", packet.isFinal ? 1 : 0);

        SecureRandom rng = new SecureRandom();
        return Fragment.FromData(packetData.toJSONString(), rng.generateSeed(4));
    }

    /**
     * Read a packet from a websocket input stream. This arrives in a fragment form so this is then transformed
     * into a packet.
     * @param in The input stream to read from
     * @return The packet obtained from the input stream
     */
    public static Packet readPacket(InputStream in) {
        try {
            Fragment frag = new Fragment(in);
            String data = (String) frag.GetData();
            JSONObject packetData = (JSONObject) new JSONParser().parse(data);
            Packet p = Packet.fromData(packetData);
            return p;

        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
