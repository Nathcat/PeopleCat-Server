import com.nathcat.peoplecat_server.Packet;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class Test {
    public static void main(String[] args) throws IOException {
        Socket s = new Socket("localhost", 1234);

        Packet ping = Packet.createPing();
        System.out.println("Created packet: \n" + ping);
        OutputStream os = s.getOutputStream();
        System.out.println(Arrays.toString(ping.payload));
        os.write(ping.getBytes());
        os.flush();


        System.out.println("Waiting...");

        Packet response = new Packet(s.getInputStream());
        System.out.println(response);

        ping = Packet.createClose();
        os.write(ping.getBytes());
        os.flush();

        s.close();
    }
}
