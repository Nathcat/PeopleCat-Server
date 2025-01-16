package com.nathcat.peoplecat_database;

import com.nathcat.peoplecat_server.Packet;
import com.sun.tools.javac.Main;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
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
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;

/**
 * Utility class for managing encryption keys
 */
public class KeyManager {
    private static final String KEY_FILE_PATH = "Assets/Data/Keys.json";
    public static final String VAPID_PUBLIC_KEY_PATH = "Assets/vapid_public.pem";
    public static final String VAPID_PRIVATE_KEY_PATH = "Assets/vapid_private.pem";

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
    public static JSONObject getUserKey(int id) throws IOException, IllegalStateException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(String.valueOf(id));

        if (userKeys == null) {
            return null;
        }

        if (!userKeys.containsKey("userKey")) {
            throw new IllegalStateException("User " + id + "'s key set does not contain a user key!");
        }

        return (JSONObject) userKeys.get("userKey");
    }

    /**
     * Get the key for a chat a user is a member of
     * @param userId The user ID
     * @param chatId The chat ID
     * @return The chat key of chat <code>chatId</code> specific to user <code>userId</code>
     */
    public static String getChatKey(int userId, int chatId) throws IOException, IllegalStateException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(String.valueOf(userId));

        if (userKeys == null) {
            return null;
        }

        if (!userKeys.containsKey("chatKeys")) {
            throw new IllegalStateException("User " + userId + "'s key set does not contain a chat key set!");
        }

        JSONObject chatKeys = (JSONObject) userKeys.get("chatKeys");
        return (String) chatKeys.get(String.valueOf(chatId));
    }

    /**
     * Initialise a new user key
     * @param userId The user ID who this key belongs to
     * @param publicKey The public key in <a href="https://developer.mozilla.org/en-US/docs/Web/API/SubtleCrypto/importKey#json_web_key">JSON Web Key</a> format.
     * @param privateKey The private key as an encrypted hex string.
     */
    public static void initUserKey(int userId, JSONObject publicKey, String privateKey) throws IOException {
        JSONObject keyFile = getKeyFile();

        JSONObject newUserSet = new JSONObject();
        JSONObject userKey = new JSONObject();
        userKey.put("publicKey", publicKey);
        userKey.put("privateKey", privateKey);
        newUserSet.put("userKey", userKey);

        keyFile.put(String.valueOf(userId), newUserSet);
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
    public static void addChatKey(int userId, int chatId, String key) throws IOException, IllegalStateException {
        JSONObject keyFile = getKeyFile();
        JSONObject userKeys = (JSONObject) keyFile.get(String.valueOf(userId));

        if (userKeys == null) {
            throw new IllegalStateException("User key set does not exist!");
        }

        JSONObject chatKeys = (JSONObject) userKeys.get("chatKeys");

        if (chatKeys == null) {
            chatKeys = new JSONObject();
        }

        chatKeys.put(String.valueOf(chatId), key);
        userKeys.put("chatKeys", chatKeys);
        keyFile.put(String.valueOf(userId), userKeys);
        writeKeyFile(keyFile);
    }

    /**
     * Read a public EC key from a PEM file
     * @param path The path to the PEM file
     * @return The EC public key contained within the PEM file
     * @throws IOException
     */
    public static PublicKey readPublicECPEM(String path) throws IOException {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(path));
        PEMParser parser = new PEMParser(isr);
        SubjectPublicKeyInfo obj = (SubjectPublicKeyInfo) parser.readObject();
        ECPublicKeyParameters publicInfo = (ECPublicKeyParameters) PublicKeyFactory.createKey(obj);
        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        ECParameterSpec publicParams = new ECParameterSpec(publicInfo.getParameters().getCurve(), publicInfo.getParameters().getG(), publicInfo.getParameters().getN(), publicInfo.getParameters().getH());
        ECPublicKeySpec ecPublicKeySpec = new ECPublicKeySpec(publicInfo.getQ(), publicParams);
        try {
            return keyFactory.generatePublic(ecPublicKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Read a private EC key from a PEM file
     * @param path The path to the PEM file
     * @return The private EC key contained within the PEM file
     * @throws IOException
     */
    public static PrivateKey readPrivateECPEM(String path) throws IOException {
        InputStreamReader isr = new InputStreamReader(new FileInputStream(path));
        PEMParser parser = new PEMParser(isr);
        PEMKeyPair kp = (PEMKeyPair) parser.readObject();
        ECPrivateKeyParameters privateInfo = (ECPrivateKeyParameters) PrivateKeyFactory.createKey(kp.getPrivateKeyInfo());

        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("EC");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        ECParameterSpec privateParams = new ECParameterSpec(privateInfo.getParameters().getCurve(), privateInfo.getParameters().getG(), privateInfo.getParameters().getN(), privateInfo.getParameters().getH());
        ECPrivateKeySpec ecPrivateKeySpec = new ECPrivateKeySpec(privateInfo.getD(), privateParams);
        try {
            return keyFactory.generatePrivate(ecPrivateKeySpec);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }
}
