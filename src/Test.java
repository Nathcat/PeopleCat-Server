import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database.MessageStore;
import com.nathcat.messagecat_database_entities.Message;
import com.nathcat.peoplecat_database.DataStore;

import java.util.Date;

public class Test {
    public static void main(String[] args) throws Exception {
        MessageStore msgStore = new MessageStore();
        MessageQueue q = msgStore.GetMessageQueue(1);

        Message msg;
        int i = 0;
        while ((msg = q.Get(i)) != null) {
            System.out.println(msg); i++;
        }
    }
}