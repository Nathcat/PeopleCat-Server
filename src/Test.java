import com.nathcat.messagecat_database.MessageQueue;
import com.nathcat.messagecat_database.MessageStore;
import com.nathcat.messagecat_database_entities.Message;

import java.util.Date;

public class Test {
    public static void main(String[] args) throws Exception {
        MessageStore store = new MessageStore();
        store.GetMessageQueue(1).Push(new Message(1, 1, new Date().getTime(), "Hello world"));
        store.WriteToFile();
    }
}