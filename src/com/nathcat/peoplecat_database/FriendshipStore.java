package com.nathcat.peoplecat_database;

import java.io.*;
import java.util.HashMap;

/**
 * @deprecated
 */
public class FriendshipStore {
    /**
     * The content of the data file
     */
    private HashMap<Integer, int[]> content;
    /**
     * The path to the data file for this store
     */
    public static final String DATA_PATH = "Assets/Data/Friends.bin";

    public FriendshipStore() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(DATA_PATH));
            content = (HashMap<Integer, int[]>) ois.readObject();

        } catch (FileNotFoundException e) {
            content = new HashMap<>();
            saveContent();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save the content of the friendship store to the data file
     */
    public void saveContent() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DATA_PATH));
            oos.writeObject(content);
            oos.flush();
            oos.close();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a user's array of friends
     * @param id The ID of the user
     * @return The array of the IDs of the user's friends
     */
    public int[] get(int id) {
        return content.get(id);
    }

    /**
     * Add a friend to a user. Automatically saves the content of the store.
     * @param id The user's ID
     * @param friendID The new friend's ID
     */
    public void addFriend(int id, int friendID) {
        int[] friends = content.get(id);
        int[] new_friends = new int[friends.length + 1];
        System.arraycopy(friends, 0, new_friends, 0, friends.length);
        new_friends[friends.length] = friendID;

        content.put(id, new_friends);
        saveContent();
    }
}
