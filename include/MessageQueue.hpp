#include <Message.hpp>

namespace PeopleCatDB {
    /**
     * @brief Contains messages in a simple queue data structure.
     * @author Nathan Baines
     */
    class MessageQueue {
        private:
        struct Node{
            Message data;
            struct Node* next;
        };

        Node* root;
        Node* end;
        unsigned int length;
        unsigned int chatID;

        public:
        MessageQueue(std::ifstream* f);
        MessageQueue(unsigned int chatID);
        MessageQueue(MessageQueue& m);
        MessageQueue(MessageQueue&& m);
        ~MessageQueue();

        MessageQueue& operator=(MessageQueue& m);
        MessageQueue& operator=(MessageQueue&& m);

        inline unsigned int getLength() {return length;}
        inline unsigned int getChatID() {return chatID;}

        /**
         * @brief Push a new message to the queue
         * @param m The message to push
         * @returns true / false depending on the result of the operation
         */
        bool push(Message m);
        /**
         * @brief Get the message at the front of the queue
         * @returns The message at the front of the queue
         */
        Message* peek();
        /**
         * @brief Return the message at the front of the queue and remove the message
         * @returns The message which was at the front of the queue
         */
        Message* pop();

        /**
         * @brief Write the message queue to a file
         * @param f The file stream to write to
         */
        void write(std::ofstream* f);
    };
};