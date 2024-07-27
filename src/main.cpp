#include <iostream>
#include <MessageQueue.hpp>
using namespace PeopleCatDB;

int main() {
    MessageQueue q((unsigned int) 0);

    Message ms[] = {{0, 1, "Hello world", 12}, {1, 0, "Hey there!", 11}};

    q.push(ms[0]);
    q.push(ms[1]);


    std::cout << q.getLength() << std::endl;
    Message* m;
    while ((m = q.pop()) != nullptr) {
        std::cout << m->content << std::endl;
        std::cout << q.getLength() << std::endl;
    }

    return 0;
}