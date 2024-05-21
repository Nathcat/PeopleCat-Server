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
     * <p>Wrapper class to allow easier access to 2 dimensional byte arrays, externally this class allows you to treat
     * such a data structure as a single array, referenced with long indexes. Internally it interprets these
     * long indexes as int indexes.</p>
     */
    public static class ByteHeap {
        private byte[][] heap;
        public final long length;

        public ByteHeap(byte[][] heap) {
            this.heap = heap;

            long l = 0;
            for (int i = 0; i < heap.length; i++) {
                l += heap[i].length;
            }

            this.length = l;
        }

        /**
         * Get a byte from the byte heap.
         * @param i Index of the byte
         * @return The byte at the given index
         * @throws ArrayIndexOutOfBoundsException Thrown if the index exceeds the size of the heap
         */
        public byte get(long i) throws ArrayIndexOutOfBoundsException {
            return heap[(int) (i >> 32)][(int) i];
        }

        /**
         * Set the byte at an index in the heap to a given byte.
         * @param i The index to set
         * @param b The byte value to set at the index
         * @throws ArrayIndexOutOfBoundsException Thrown if the index exceeds the size of the heap
         */
        public void set(long i, byte b) throws ArrayIndexOutOfBoundsException {
            heap[(int) (i >> 32)][(int) i] = b;
        }
    }

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
        public final ByteHeap payload;
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
            byte length_descriptor = (byte) (payload_len & 0x7F);
            long length = 0;
            boolean payloadMasked = (payload_len & 0x80) == 0x80;

            if (length_descriptor >= 0 && length_descriptor <= 125) {
                length = length_descriptor;
            }
            else if (length_descriptor == 126) {
                byte[] buffer = stream.readNBytes(2);

                length |= ((0xFF & ((long) buffer[0])) << 8);
                length |= (0xFF & ((long) buffer[1]));
            }
            else if (length_descriptor == 127){
                byte[] buffer = stream.readNBytes(8);
                for (int i = 7; i >= 0; i--) {
                    length |= ((0xFF & ((long) buffer[i])) << (i * 8));
                }
            }
            else {
                throw new RuntimeException("Invalid length descriptor: " + length_descriptor);
            }


            if (payloadMasked) {
                key = stream.readNBytes(4);
                payload = DecodeBuffer(stream, key, length);
            }
            else {
                int[] l = {(int) length};
                if ((length >> 32) > 0) {
                    l = new int[] {(int) length, (int) (length >> 32)};
                }
                byte[][] buffers = new byte[l.length][];
                for (int i = 0; i < buffers.length; i++) {
                    buffers[i] = stream.readNBytes(l[i]);
                }

                payload = new ByteHeap(buffers);
            }
        }

        /**
         * Decode the masked payload buffer
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.3">RFC 6455 - Client-To-Server Masking</a>
         * @param stream The stream, should include both the key and the masked payload
         * @return Decoded payload buffer
         * @throws IOException
         */
        private static ByteHeap DecodeBuffer(InputStream stream, byte[] key, long l) throws IOException {
            int[] length = {(int) l};
            if ((l >> 32) > 0) {
                length = new int[] {(int) l, (int) (l >> 32)};
            }

            byte[][] buffers = new byte[length.length][];

            for (int i = 0; i < buffers.length; i++) {
                byte[] buffer = stream.readNBytes(length[i]);
                for (int j = 0; j < buffer.length; j++) {
                    buffer[j] = (byte) (buffer[j] ^ key[j % 4]);
                }

                buffers[i] = buffer;
            }

            return new ByteHeap(buffers);
        }

        /**
         * Use the opcode to interpret the payload buffer into a Java object which can be used in a program.
         * @see <a href="https://datatracker.ietf.org/doc/html/rfc6455#section-5.2">RFC 6455 - Base Framing Protocol</a>
         * @return Java interpretation of the payload buffer
         */
        public Object GetData() {
            if (opcode == 1) {
                StringBuilder sb = new StringBuilder();
                for (long i = 0; i < payload.length; i++) {
                    sb.append((char) payload.get(i));
                }
                return sb.toString();
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
                extra_length = new byte[] {(byte) ((payload.length & 0xFF00) >> 8), (byte) (payload.length & 0xFF)};
            }
            else {
                length_desc = 127;
                extra_length = new byte[8];
                for (int i = 7; i >= 0; i--) {
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
        if (data.contentEquals("Not websock")) return false;
        Matcher get = Pattern.compile("^GET").matcher(data);

        upgradeSocket(socket, data);

        return get.find();
    }

    /**
     * Upgrade a TCP socket to a web socket.
     * @param socket The socket to upgrade
     * @throws IOException Thrown when an I/O operation fails
     */
    private static void upgradeSocket(Socket socket, String data) throws IOException {
        OutputStream out = socket.getOutputStream();

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
        if (packetData == null) packetData = new JSONObject();
        packetData.put("type", packet.type);
        packetData.put("isFinal", packet.isFinal ? 1 : 0);

        SecureRandom rng = new SecureRandom();
        return Fragment.FromData(packetData.toJSONString(), rng.generateSeed(4));
    }

    public static String byteBinStr(byte b) {
        return String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
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
