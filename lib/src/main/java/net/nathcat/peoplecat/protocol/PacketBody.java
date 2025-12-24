package net.nathcat.peoplecat.protocol;

import com.google.gson.Gson;

/**
 * Serves as a base class for all packet bodies.
 *
 */
public abstract class PacketBody {
  public static class Empty extends PacketBody {
  }

  /**
   * Encode this class to a JSON string byte array
   *
   * @return The encoded JSON string as a byte array
   */
  public byte[] toBytes() {
    Gson gson = new Gson();
    return gson.toJson(this).getBytes();
  }

  public int length() {
    return toBytes().length;
  }
}
