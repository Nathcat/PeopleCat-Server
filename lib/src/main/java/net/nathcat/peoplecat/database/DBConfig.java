package net.nathcat.peoplecat.database;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;

import com.google.gson.Gson;

/**
 * Presents the database configuration.
 * This is to be loaded upon the start up of the program.
 */
public class DBConfig {
  public static DBConfig instance;

  public static void load(String path) throws FileNotFoundException {
    FileInputStream fis = new FileInputStream(path);
    Gson gson = new Gson();

    instance = gson.fromJson(new InputStreamReader(fis), DBConfig.class);
  }

  /**
   * The directory which is to contain all of the chat message queue files.
   */
  public String chatContentsDirectory;
  /**
   * The time after which a message will expire. Measured as an epoch (ms)
   */
  public long messageExpiryTime;
}
