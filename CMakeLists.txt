cmake_minimum_required(VERSION 3.22.1)

project(PeopleCat-Server)

add_executable(PeopleCat-Server src/main.cpp)

set_target_properties(PeopleCat-Server PROPERTIES RUNTIME_OUTPUT_DIRECTORY "${CMAKE_SOURCE_DIR}/bin")