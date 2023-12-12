package com.nathcat.peoplecat_server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

/**
 * Class for creating data which can be sent / received by the server / client.
 *
 * @author Nathan Baines
 */
public class Packet {
    public int type;
    public int payloadType;
    public byte[] payload;

    public Packet(InputStream inStream) {
        Scanner input = new Scanner(inStream);
        type = input.nextInt();
        payloadType = input.nextInt();
        int length = input.nextInt();
        payload = new byte[length];

        try {
            int res = inStream.read(payload);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
