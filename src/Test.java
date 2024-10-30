import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.IPacketHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

public class Test {
    public static void main(String[] args) throws Exception {
        TestHandler t = new TestHandler();

        while (true) {}
    }
}

class TestHandler extends ConnectionHandler {

    public TestHandler() throws IOException {
        super(new Socket("localhost", 1234), new IPacketHandler() {
            @Override
            public Packet[] error(ConnectionHandler handler, Packet[] packets) {
                System.out.println(packets[0].getData().toJSONString());
                return new Packet[0];
            }

            @Override
            public Packet[] ping(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] authenticate(ConnectionHandler handler, Packet[] packets) {
                System.out.println("Authenticated with response: " + packets[0].getData().toJSONString());

                JSONObject p = new JSONObject();
                p.put("NewPath", "NULL");
                handler.writePacket(Packet.createPacket(Packet.TYPE_CHANGE_PFP_PATH, true, p));
                return new Packet[0];
            }

            @Override
            public Packet[] createNewUser(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] close(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getUser(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getMessageQueue(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] sendMessage(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] notificationMessage(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] joinChat(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] changeProfilePicture(ConnectionHandler handler, Packet[] packets) {
                System.out.println(packets[0] + " -> " + packets[0].getData());
                return new Packet[0];
            }

            @Override
            public Packet[] getActiveUserCount(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }
        });
    }

    @Override
    public void run() {
        try {
            outStream.write("Not websock\r\n\r\n".getBytes());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        JSONObject auth_packet = new JSONObject();
        auth_packet.put("Username", "TestUsername");
        auth_packet.put("Password", "1234");
        writePacket(Packet.createPacket(Packet.TYPE_AUTHENTICATE, true, auth_packet));

        try {
            while (true) {
                // Get the packet sequence from the input stream
                ArrayList<Packet> packets = new ArrayList<>();
                Packet p;
                while (!(p = getPacket()).isFinal) {
                    log("Got packet:\n" + p);

                    if (p.type == Packet.TYPE_CLOSE) {
                        break;
                    }

                    packets.add(p);
                }

                log("Got packet:\n" + p);

                if (p.type == Packet.TYPE_CLOSE) {
                    break;
                }

                packets.add(p);


                Packet[] packetSequence = packets.toArray(new Packet[0]);

                // Use a response handler to determine the response from the packet sequence
                Packet[] responseSequence = packetHandler.handle(this, packetSequence);
                if (responseSequence == null) continue;

                // Send the response sequence to the client through the output stream
                for (Packet packet : responseSequence) {
                    if (packet.type != Packet.TYPE_PING) log("Writing packet: \n" + packet + " -> " + packet.getData().toJSONString());
                    else log("Pinging client.");
                    writePacket(packet);
                }
            }
        } catch (Exception e) { log("\033[91;3m" + e.getMessage() + "\n" + Server.stringifyStackTrace(e.getStackTrace()) + "\033[0m"); }

        log("Closing thread.");
        close();
        active = false;
        interrupt();
    }
}