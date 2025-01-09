package com.nathcat.peoplecat_database;

import com.nathcat.peoplecat_server.Packet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for managing encryption keys
 */
public class KeyManager {
    private static final String KEY_FILE_PATH = "Assets/Data/Keys.json";

    /**
     * Get the key file, if it does not exist, initialise the key file with an empty JSON set.
     * @return The <code>JSONObject</code> parsed from the key file.
     */
    private static JSONObject getKeyFile() throws IOException {
        try (FileInputStream fis = new FileInputStream(KEY_FILE_PATH)) {
            return (JSONObject) new JSONParser().parse(new String(fis.readAllBytes(), StandardCharsets.UTF_8));
        }
        catch (FileNotFoundException e) {
            // Initialise the key file and try again
            writeKeyFile(new JSONObject());
            return getKeyFile();
        }
        catch (ParseException e) {  // This shouldn't happen
            throw new RuntimeException(e);
        }
    }

    /**
     * Write the contents of a new key structure to the key file
     * @param contents The new the key structure
     */
    private static void writeKeyFile(JSONObject contents) throws IOException {
        FileOutputStream fos = new FileOutputStream(KEY_FILE_PATH);
        fos.write(contents.toJSONString().getBytes(StandardCharsets.UTF_8));
        fos.close();
    }

    /**
     * Get a user's public key
     * @param id The user's ID
     * @return The user's public key in <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> format, or null if there is no key, an invalid number of keys, or an SQL exception occurs.
     */
    public static JSONObject getUserKey(int id) throws IOException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(id);

        if (userKeys == null) {
            return null;
        }

        if (!userKeys.containsKey("userKey")) {
            throw new RuntimeException("User " + id + "'s key set does not contain a user key!");
        }

        return (JSONObject) userKeys.get("userKey");
    }

    /**
     * Get the key for a chat a user is a member of
     * @param userId The user ID
     * @param chatId The chat ID
     * @return The chat key of chat <code>chatId</code> specific to user <code>userId</code>
     */
    public static JSONObject getChatKey(int userId, int chatId) throws IOException, IllegalStateException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(userId);

        if (userKeys == null) {
            return null;
        }

        if (!userKeys.containsKey("chatKeys")) {
            throw new IllegalStateException("User " + userId + "'s key set does not contain a chat key set!");
        }

        JSONObject chatKeys = (JSONObject) userKeys.get("chatKeys");
        return (JSONObject) chatKeys.get(chatId);
    }

    /**
     * Initialise a new user key
     * @param userId The user ID who this key belongs to
     * @param key The key in <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> format.
     */
    public static void initUserKey(int userId, JSONObject key) throws IOException {
        JSONObject keyFile = getKeyFile();

        JSONObject newUserSet = new JSONObject();
        newUserSet.put("userKey", key);

        keyFile.put(userId, newUserSet);
        writeKeyFile(keyFile);
    }

    /**
     * Add a new chat key to a user's key set. Please refer to <code>Packet.TYPE_JOIN_CHAT</code> docs for chat key
     * specification.
     * @param userId The ID of the user who will own the key
     * @param chatId The ID of the chat this key is for
     * @param key The key itself
     * @see Packet#TYPE_JOIN_CHAT
     */
    public static void addChatKey(int userId, int chatId, JSONObject key) throws IOException, IllegalStateException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(userId);

        if (userKeys == null) {
            throw new IllegalStateException("User key set does not exist!");
        }

        JSONObject chatKeys = (JSONObject) userKeys.get("chatKeys");

        if (chatKeys == null) {
            chatKeys = new JSONObject();
        }

        chatKeys.put(chatId, key);
        userKeys.put("chatKeys", chatKeys);
        keyFile.put(userId, userKeys);
        writeKeyFile(keyFile);
    }
}
