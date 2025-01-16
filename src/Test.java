import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database.MessageStore;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.IPacketHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;
import nl.martijndwars.webpush.Base64Encoder;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.jose4j.json.JsonUtil;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.Socket;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

public class Test {
    public static char[] nibbleToHex = new char[] {
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static void main(String[] args) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        InputStreamReader isr = new InputStreamReader(new FileInputStream(KeyManager.VAPID_PUBLIC_KEY_PATH));
        PEMParser parser = new PEMParser(isr);
        SubjectPublicKeyInfo obj = (SubjectPublicKeyInfo) parser.readObject();
        ECPublicKeyParameters publicInfo = (ECPublicKeyParameters) PublicKeyFactory.createKey(obj);

        byte[] x = publicInfo.getQ().getXCoord().toBigInteger().toByteArray();
        byte[] y = publicInfo.getQ().getYCoord().toBigInteger().toByteArray();
        byte[] buffer = new byte[65];

        System.out.println(Arrays.toString(x));
        System.out.println(x.length);
        System.out.println(Arrays.toString(y));
        System.out.println(y.length);
        buffer[0] = 4;
        System.arraycopy(x, 0, buffer, 1 , 32);
        System.arraycopy(y, 1, buffer, 33, 32);

        System.out.println(Arrays.toString(buffer));

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < buffer.length; i++) {
            if (i != 0) sb.append(':');
            sb.append(nibbleToHex[(buffer[i] & 0xF0) >> 4]);
            sb.append(nibbleToHex[buffer[i] & 0xF]);
        }

        System.out.println(sb.toString());
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
                return new Packet[0];
            }

            @Override
            public Packet[] getActiveUserCount(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] notificationUserOnline(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] notificationUserOffline(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getFriends(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] friendRequest(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getServerInfo(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getChatMemberships(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] createChat(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] initUserKey(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] getUserKey(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] addToChat(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] pushSubscribe(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }

            @Override
            public Packet[] pushUnsubscribe(ConnectionHandler handler, Packet[] packets) {
                return new Packet[0];
            }
        });
    }

    @Override
    public void run() {
        JSONObject auth_packet = new JSONObject();
        auth_packet.put("cookie-auth", "cpkjs5inil4mr6ooprvmqr4vj4");
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