package com.nathcat.peoplecat_server.handlers;

import java.io.FileNotFoundException;
import java.util.Date;

import org.json.simple.JSONObject;

import com.nathcat.peoplecat_database.KeyManager;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetServerInfo implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        JSONObject d = new JSONObject();
        d.put("version", Server.version);
        d.put("serverTime", new Date().toString());
        try {
            d.put("pushServicePublicKey", KeyManager.ecPublicKeyUncompressed(KeyManager.VAPID_PUBLIC_KEY_PATH));
        } catch (FileNotFoundException e) {
            d.put("pushServicePublicKey", null);
        }

        return new Packet[] { Packet.createPacket(
                Packet.TYPE_GET_SERVER_INFO,
                true,
                d) };                
    }
    
}
