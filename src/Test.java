import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database.MessageStore;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.DataStore;

import java.util.Date;

public class Test {
    public static void main(String[] args) throws Exception {
        DataStore<Integer, int[]> ds = new DataStore<>("Assets/Data/ChatMemberships.bin");
        ds.set(1, new int[0]);
    }
}