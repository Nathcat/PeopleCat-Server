package com.nathcat.peoplecat_database;

import java.io.*;
import java.util.HashMap;

/**
 * A class which is intended to provided a wrapper for data files which contain a hashmap
 * @param <K> The key type
 * @param <V> The value type
 */
public class DataStore<K, V> {
    private HashMap<K, V> content;
    public final String dataPath;

    public DataStore(String dataPath) {
        this.dataPath = dataPath;

        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(this.dataPath));
            content = (HashMap<K, V>) ois.readObject();

        } catch (FileNotFoundException e) {
            content = new HashMap<>();
            saveContent();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Save the content of the file
     */
    public void saveContent() {
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(this.dataPath));
            oos.writeObject(content);
            oos.flush();
            oos.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get a value from the file
     * @param k The key to get
     * @return The value attached to the key, or null if there is none
     */
    public V get(K k) {
        return content.get(k);
    }

    /**
     * Remove a value from the file
     * @param k The key to remove
     * @return The value which was attached to the key, if there is one
     */
    public V remove(K k) {
        V v = content.remove(k);
        saveContent();
        return v;
    }

    /**
     * Set the value attached to a key
     * @param k The key to set
     * @param v The value to attach to the key
     */
    public void set(K k, V v) {
        content.put(k, v);
        saveContent();
    }
}
