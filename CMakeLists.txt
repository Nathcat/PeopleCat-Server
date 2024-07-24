cmake_minimum_required(VERSION 3.22.1)

project(PeopleCat-Server)

add_executable(PeopleCat-Server src/main.cpp)

set_target_properties(PeopleCat-Server 
    PROPERTIES 
    RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/bin"
)

include_directories(PeopleCat-Server 
    "${CMAKE_SOURCE_DIR}/libs/mysql-connector-c++-9.0.0-linux-glibc2.28-x86-64bit/include"
)

target_link_libraries(PeopleCat-Server
    "${CMAKE_SOURCE_DIR}/libs/mysql-connector-c++-9.0.0-linux-glibc2.28-x86-64bit/lib64/libmysqlcppconn.so"
)