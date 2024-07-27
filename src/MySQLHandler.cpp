/// MySQLHandler.cpp
///
/// Implementation of MySQLHandler.
/// @author Nathan Baines

#include <MySQLHandler.hpp>

using namespace PeopleCatDB;

MySQLHandler::MySQLHandler(const char* host, const char* username, const char* password) {
    driver = get_driver_instance();
    con = driver->connect(host, username, password);
}

sql::ResultSet* MySQLHandler::query(const char* q) {
    sql::Statement* stmt = con->createStatement();
    sql::ResultSet* res = stmt->executeQuery(q);
    stmt->close();
    delete stmt;
    return res;
}

void MySQLHandler::update(const char* q) {
    sql::Statement* stmt = con->createStatement();
    stmt->executeQuery(q);
    stmt->close();
    delete stmt;
}