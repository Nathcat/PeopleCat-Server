#include <MessageQueue.hpp>
#include <string.h>
using namespace PeopleCatDB;

MessageQueue::MessageQueue(std::ifstream* f) {
    f->read((char*) &chatID, sizeof(unsigned int));
    unsigned int l = 0;
    f->read((char*) &l, sizeof(unsigned int));

    for (unsigned int i = 0; i < l; i++) {
        push(readMessage(f));
    }
}

MessageQueue::MessageQueue(unsigned int chatID) {
    this->chatID = chatID;
    this->root = nullptr;
    this->end = nullptr;
    this->length = 0;
}

MessageQueue::MessageQueue(MessageQueue& m) {
    memcpy(this, &m, sizeof(MessageQueue));
}

MessageQueue::MessageQueue(MessageQueue&& m) {
    memcpy(this, &m, sizeof(MessageQueue));
    m.root = nullptr;
    m.end = nullptr;
}

MessageQueue::~MessageQueue() {
    delete root;
    delete end;
}

MessageQueue& MessageQueue::operator=(MessageQueue& m) {
    memcpy(this, &m, sizeof(MessageQueue));
    return *this;
}

MessageQueue& MessageQueue::operator=(MessageQueue&& m) {
    memcpy(this, &m, sizeof(MessageQueue));
    m.root = nullptr;
    m.end = nullptr;
    return *this;
}

bool MessageQueue::push(Message m) {
    if (length == 20) {
        return false;
    }

    struct Node* n = (struct Node*) malloc(sizeof(struct Node));
    n->data = m;
    n->next = nullptr;
    
    if (length != 0) {
        end->next = n;
        end = n;
    }
    else {
        root = n;
        end = n;
    }
    
    length++;
    return true;
}

Message* MessageQueue::peek() {
    return &root->data;
}

Message* MessageQueue::pop() {
    if (length == 0) return nullptr;
    
    Message* m = &root->data;
    root = root->next;
    length--;

    return m;
}

void MessageQueue::write(std::ofstream* f) {
    f->write((const char*) &chatID, sizeof(unsigned int));
    f->write((const char*) &length, sizeof(unsigned int));
    
    Node* n = root;
    while (n != nullptr) {
        writeMessage(f, n->data);
        n = n->next;
    }
}