package net.nathcat.peoplecat.protocol;

import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;

/**
 * Serves as a base class for all packet bodies.
 *
 */
public abstract class PacketBody {
  public static class Empty extends PacketBody {
  }

  /**
   * Encode this class to a JSON string byte array. Note that this is encoded as a
   * UTF 8 string.
   *
   * @return The encoded JSON string as a byte array
   */
  public byte[] toBytes() {
    Gson gson = new Gson();
    return gson.toJson(this).getBytes(StandardCharsets.UTF_8);
  }

  public int length() {
    return toBytes().length;
  }
}
