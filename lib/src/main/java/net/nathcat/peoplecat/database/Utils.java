package net.nathcat.peoplecat.database;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

import net.nathcat.peoplecat.database.types.DBType;

/**
 * General database utilities.
 */
public final class Utils {
  private Utils() {
  }

  /**
   * Extract the results of a result set into an array of DBTypes.
   * 
   * @param rs The result set
   * @param tC The class of the target DBType
   * @returns An array of DBTypes specified by tC
   */
  @SuppressWarnings("unchecked")
  public static <T extends DBType> T[] extractResults(ResultSet rs, Class<T> tC)
      throws SQLException, InstantiationException, IllegalAccessException, NoSuchMethodException,
      InvocationTargetException, NoSuchFieldException {
    ResultSetMetaData md = rs.getMetaData();
    ArrayList<T> l = new ArrayList<T>();

    while (rs.next()) {
      T row = tC.getConstructor().newInstance();
      l.add(row);

      for (int i = 0; i < md.getColumnCount(); i++) {
        String colLabel = md.getColumnLabel(i + 1);
        tC.getField(colLabel).set(row, rs.getObject(colLabel));
      }
    }

    return l.toArray((T[]) Array.newInstance(tC, 0));
  }

  /**
   * Create a {@link DBType} object from a {@link JSONObject}.
   *
   * @param json The {@link JSONObject} to source data from
   * @param tC   The {@link DBType}
   * @return The resulting {@link DBType}.
   */
  public static <T extends DBType> T typeFromJson(JSONObject json, Class<T> tC) throws InstantiationException,
      IllegalAccessException, InvocationTargetException, NoSuchFieldException, NoSuchMethodException {
    T res = tC.getConstructor().newInstance();

    for (Object key : json.keySet()) {
      tC.getField((String) key).set(res, json.get(key));
    }

    return res;
  }
}
