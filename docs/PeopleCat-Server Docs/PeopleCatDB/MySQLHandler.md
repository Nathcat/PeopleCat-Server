```cpp
class PeopleCatDB::MySQLHandler
```
This class simplifies the process of connecting to and manipulating the data within a MySQL server.

In the PeopleCat server, the config data for the MySQL server should be retrieved from `Assets/MySQL_Config.json`.
## Constructors
```cpp
MySQLHandler(const char* host, const char* username, const char* password);
```

## Public methods
```cpp
sql::ResultSet* query(const char* q);`
```
> Perform a query which you can expect a result set from
> _**q**_ - The query to send to the server
>
> Returns the result set returned from the server.

```cpp
void update(const char* q);
```
> Perform a query which you do not expect a result set from.
> _**q**_ - The query to send to the server

