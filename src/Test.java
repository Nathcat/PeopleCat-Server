import com.google.gson.Gson;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.IPacketHandler;
import com.nathcat.peoplecat_server.Packet;
import org.json.simple.JSONObject;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;

public class Test {
    private class TestHandler extends ConnectionHandler {

        public TestHandler(Socket client) throws IOException {
            super(client, new IPacketHandler() {
                @Override
                public Packet[] error(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got error packets:");
                    for (Packet p : packets) {
                        System.out.println(p + " -> " + p.getData().toJSONString());
                    }

                    return null;
                }

                @Override
                public Packet[] ping(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got ping packets:");
                    for (Packet p : packets) {
                        System.out.println(p);
                    }

                    return new Packet[] { Packet.createPing() };
                }

                @Override
                public Packet[] authenticate(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got Auth packets:");
                    for (Packet p : packets) {
                        System.out.println(p + " -> " + p.getData().toJSONString());
                    }

                    JSONObject userRequestData = new JSONObject();
                    userRequestData.put("display_name", "Nathcat");

                    handler.writePacket(Packet.createPacket(
                            Packet.TYPE_GET_USER,
                            true,
                            userRequestData
                    ));

                    return null;
                }

                @Override
                public Packet[] createNewUser(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got new user packets:");
                    for (Packet p : packets) {
                        System.out.println(p + " -> " + p.getData().toJSONString());
                    }

                    handler.writePacket(Packet.createClose());

                    return null;
                }

                @Override
                public Packet[] close(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got close packets:");
                    for (Packet p : packets) {
                        System.out.println(p);
                    }

                    return null;
                }

                @Override
                public Packet[] getUser(ConnectionHandler handler, Packet[] packets) {
                    handler.log("Got user packets:");
                    for (Packet p : packets) {
                        System.out.println(p + " -> " + p.getData().toJSONString());
                    }

                    return null;
                }
            });
        }

        @Override
        public void run() {
            try {
                outStream.write("Not websock\n".getBytes());
                outStream.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            JSONObject authData = new JSONObject();

            try {
                MessageDigest sha = MessageDigest.getInstance("SHA-256");
                byte[] digest = sha.digest("hello1234".getBytes());
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) {
                    hexString.append(String.format("%02x", b));
                }

                authData.put("password", hexString.toString());
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }

            authData.put("username", "nathcat6654");
            assert authData.containsKey("password");
            authData.put("display_name", "Nathan");

            System.out.println("Created packet");

            writePacket(Packet.createPacket(
                    Packet.TYPE_CREATE_NEW_USER,
                    true,
                    authData
            ));

            System.out.println("Written packet");

            while (true) {
                ArrayList<Packet> packets = new ArrayList<>();
                Packet p;
                while (!(p = getPacket()).isFinal) {
                    if (p.type == Packet.TYPE_CLOSE) break;

                    packets.add(p);
                }

                if (p.type == Packet.TYPE_CLOSE) break;

                packets.add(p);

                packetHandler.handle(this, packets.toArray(new Packet[0]));
            }
        }
    }
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 1234);
        Test t = new Test(s);

        while (true) {}
    }

    public TestHandler handler;
    public Test(Socket s) throws IOException {
        handler = new TestHandler(s);
    }
}
