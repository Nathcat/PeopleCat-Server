```cpp
class PeopleCatDB::MessageQueue
```
Contains messages from a chat in a simple queue data structure.

## Constructors
```cpp
MessageQueue(std::ifstream* f);
```

```cpp
MessageQueue(unsigned int chatID);
```

## Public methods
```cpp
inline unsigned int getLength();
```
> Gets the current length of the queue.

```cpp
bool push(PeopleCatDB::Message m);
```
> Push a new message to the queue.
> _**m**_ - The message to push
>
> Returns true / false depending on the result of the operation.

```cpp
Message* peek();
```
> Return the message at the front of the queue

```cpp
Message* pop();
```
> Return the message at the front of the queue, and also remove it from the queue.

```cpp
void write(std::ofstream* f);
```
> Write a message queue to a file.
