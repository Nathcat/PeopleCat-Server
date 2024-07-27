#include <fstream>
#include <stdlib.h>
#include <string.h>

namespace PeopleCatDB {
    /**
     * @brief An object representing a message which was sent into a chat by a user
     * @author Nathan Baines
     */
    class Message{
        public:
        unsigned int senderID;
        unsigned int chatID;
        const char* content;
        unsigned long long contentLength;

        inline Message(unsigned int senderID, unsigned int chatID, const char* content, unsigned long long contentLength) {
            this->senderID = senderID;
            this->chatID = chatID;
            this->content = content;
            this->contentLength = contentLength;
        }

        inline Message(Message& m) {
            memcpy(this, &m, sizeof(Message));
        }

        inline Message(Message&& m) {
            memcpy(this, &m, sizeof(Message));
            m.content = nullptr;
        }

        inline Message& operator=(Message& m) {
            memcpy(this, &m, sizeof(Message));
            return *this;
        }

        inline Message& operator=(Message&& m) {
            memcpy(this, &m, sizeof(Message));
            m.content = nullptr;
            return *this;
        }
    };

    /**
     * @brief Write a message object to a file stream.
     * @param f The file stream to write to.
     * @param m The message to write to the stream.
     */
    inline void writeMessage(std::ofstream* f, Message m) {
        f->write((const char*) &m.senderID, sizeof(unsigned int));
        f->write((const char*) &m.chatID, sizeof(unsigned int));
        f->write((const char*) &m.contentLength, sizeof(unsigned long long));
        f->write(m.content, sizeof(char) * m.contentLength);
    }

    /**
     * @brief Read a message object from a file stream.
     * @param f The file stream to read from.
     */
    inline Message readMessage(std::ifstream* f) {
        Message m = {0, 0, 0, 0};
        f->read((char*) &m.senderID, sizeof(unsigned int));
        f->read((char*) &m.chatID, sizeof(unsigned int));
        f->read((char*) &m.contentLength, sizeof(unsigned long long));
        m.content = (const char*) malloc(sizeof(char) * m.contentLength);
        f->read((char*) m.content, sizeof(char) * m.contentLength);
        return m;
    }
};