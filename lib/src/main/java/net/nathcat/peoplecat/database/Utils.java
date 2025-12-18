package net.nathcat.peoplecat.database;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;

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
}
