import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database.MessageStore;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.DataStore;

import java.util.Date;

public class Test {
    public static void main(String[] args) throws Exception {
        /*MessageStore msgStore = new MessageStore();
        msgStore.AddMessageQueue(new MessageQueue(1));
        msgStore.WriteToFile();*/
        DataStore<Integer, int[]> m = new DataStore<>("Assets/Data/ChatMemberships.bin");
        m.set(1, new int[0]);
        m.saveContent();
    }
}