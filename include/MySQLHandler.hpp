#ifndef MYSQLHANDLER_HPP
#define MYSQLHANDLER_HPP

#include <mysql/jdbc.h>

namespace PeopleCatDB {
    /** @brief A class which handles and simplifies a lot of the process of connecting to and querying data from a MySQL server.
     *  @author Nathan Baines
     */
    class MySQLHandler {
        private:
        sql::Driver *driver;
        sql::Connection *con;

        public:
        MySQLHandler(const char* host, const char* username, const char* password);

        /** @brief Perform a query which you expect a result set from.
         *  @param q The query to send to the server
         *  @return The returned result set
         */
        sql::ResultSet* query(const char* q);
        
        /** @brief Perform a query which you do not expect a result set from.
         *  @param q The query to send to the server
         */
        void update(const char* q);
    };
};

#endif