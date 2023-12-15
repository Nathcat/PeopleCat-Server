import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.IPacketHandler;
import com.nathcat.peoplecat_server.Packet;
import org.json.simple.JSONObject;

import java.io.*;
import java.net.Socket;
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
            });
        }

        @Override
        public void run() {
            JSONObject data = new JSONObject();
            data.put("username", "nathcat6654");
            data.put("password", "hello1234");
            data.put("display_name", "Nathcat");

            writePacket(Packet.createPacket(
                    Packet.TYPE_CREATE_NEW_USER,
                    true,
                    data
            ));

            ArrayList<Packet> packets = new ArrayList<>();
            Packet p;
            while (!(p = getPacket()).isFinal) {
                if (p.type == Packet.TYPE_CLOSE) { break; }

                packets.add(p);
            }

            packets.add(p);

            packetHandler.handle(this, packets.toArray(new Packet[0]));
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
