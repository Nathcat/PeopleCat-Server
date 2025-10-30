package com.nathcat.peoplecat_server.handlers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import org.json.simple.JSONObject;

import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.MessageBox;
import com.nathcat.peoplecat_server.ConnectionHandler;
import com.nathcat.peoplecat_server.Packet;
import com.nathcat.peoplecat_server.Server;

public class GetMessageQueue implements IPacketHandler {

    @Override
    public Packet[] handle(Server server, ConnectionHandler handler, Packet[] packets) {
        if (!handler.authenticated)
            return new Packet[] { Packet.createError("Not authenticated",
                    "This request requires you to have an authenticated connection.") };
        if (packets.length > 1)
            return new Packet[] { Packet.createError("Invalid data type",
                    "Get message queue request does not accept multi-packet arrays.") };

        JSONObject request = packets[0].getData();
        if (request.get("chatId") == null) {
            return new Packet[] {
                    Packet.createError("Incorrect data provided", "You must provide the ChatID.") };
        }

        int chatId = Math.toIntExact((long) request.get("chatId"));

        if (!server.db.isMemberOfChat((int) handler.user.get("id"), chatId))
            return new Packet[] { Packet.createError("Not member of chat",
                    "You are not a member of this chat!") };

        Message[] messages;
        try {
            messages = MessageBox.openMessageBox(chatId);
        } catch (FileNotFoundException e) {
            try {
                MessageBox.updateMessageBox(chatId, new Message[0]);
                messages = MessageBox.openMessageBox(chatId);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ArrayList<Packet> response = new ArrayList<>();
        JSONObject preResponse = new JSONObject();
        preResponse.put("messageCount", messages.length);

        response.add(Packet.createPacket(
                Packet.TYPE_GET_MESSAGE_QUEUE,
                false,
                preResponse));

        for (JSONObject m : Arrays.stream(messages).map(MessageBox::messageToJSON).toList()) {
            response.add(Packet.createPacket(
                    Packet.TYPE_GET_MESSAGE_QUEUE,
                    false,
                    m));
        }

        if (messages.length == 0) {
            return new Packet[] { Packet.createError("No messages", "There are no messages in this chat.") };
        }

        response.get(response.size() - 1).isFinal = true;

        return response.toArray(new Packet[0]);
    }

}
