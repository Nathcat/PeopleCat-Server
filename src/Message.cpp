#include <Message.hpp>
using namespace PeopleCatDB;

void PeopleCatDB::writeMessage(std::ofstream* f, Message m) {
    f->write((const char*) &m.senderID, sizeof(unsigned int));
    f->write((const char*) &m.chatID, sizeof(unsigned int));
    f->write((const char*) &m.contentLength, sizeof(unsigned long long));
    f->write(m.content, sizeof(char) * m.contentLength);
}

Message PeopleCatDB::readMessage(std::ifstream* f) {
    Message m = {0, 0, 0, 0};
    f->read((char*) &m.senderID, sizeof(unsigned int));
    f->read((char*) &m.chatID, sizeof(unsigned int));
    f->read((char*) &m.contentLength, sizeof(unsigned long long));
    m.content = (const char*) malloc(sizeof(char) * m.contentLength);
    f->read((char*) m.content, sizeof(char) * m.contentLength);
    return m;
}