package net.nathcat.peoplecat.protocol;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

/**
 * <p>
 * Used to encapsulate / encode / decode data which is sent / received from the
 * program to other parts of PeopleCat.
 * </p>
 *
 * <h3>Specification</h3>
 * <table>
 * <tr>
 * <th>Byte count</th>
 * <th>Data type</th>
 * <th>Purpose</th>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>Packet ID, used to link requests and responses together.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>Packet type</td>
 * </tr>
 * <tr>
 * <td>1</td>
 * <td>Boolean</td>
 * <td>True if this is the final packet in the sequence. If false, more related
 * packets will follow.</td>
 * </tr>
 * <tr>
 * <td>4</td>
 * <td>Integer</td>
 * <td>The length in bytes of the packet's payload</td>
 * </tr>
 * </table>
 * 
 * <p>
 * Any data provided in a packet following the above data is specified to the
 * type of packet.
 * </p>
 */
public abstract class Packet {
  public final int id;
  public final Type type;
  public final boolean isFinal;

  /**
   * Construct a new packet
   *
   * @param id
   * @param type
   * @param isFinal
   */
  protected Packet(int id, Type type, boolean isFinal) {
    this.id = id;
    this.type = type;
    this.isFinal = isFinal;
  }

  /**
   * Construct a new packet with a random ID.
   *
   * @param type
   * @param isFinal
   */
  protected Packet(Type type, boolean isFinal) {
    this.type = type;
    this.isFinal = isFinal;
    this.id = new Random().nextInt();
  }

  /**
   * Construct a packet with a random ID, and set this packet to be a final
   * packet.
   *
   * @param type
   */
  protected Packet(Type type) {
    this.type = type;
    isFinal = true;
    id = new Random().nextInt();
  }

  /**
   * Construct a new packet set as a final packet.
   *
   * @param id
   * @param type
   */
  protected Packet(int id, Type type) {
    this.id = id;
    this.type = type;
    this.isFinal = true;
  }

  /**
   * Get this packet's encoded byte stream
   */
  protected ByteArrayOutputStream getByteStream() {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try {
      DataOutputStream dos = new DataOutputStream(baos);
      dos.writeInt(id);
      dos.writeInt(type.typeId);
      dos.writeBoolean(isFinal);
      dos.writeInt(length());
    } catch (IOException e) {
      // This shouldn't happen
      throw new RuntimeException(e);
    }

    return baos;
  }

  /**
   * Get the binary composition of this packet
   *
   * @return This packet encoded into an array of bytes.
   */
  public byte[] getBytes() {
    return getByteStream().toByteArray();
  }

  /**
   * Get the length of this packet's payload
   */
  public int length() {
    return 0;
  }
}
