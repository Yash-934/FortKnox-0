cmake_minimum_required(VERSION 3.22.1)
project("fortknox")

add_library(
        fortknox
        SHARED
        native-lib.cpp)

find_library(
        log-lib
        log)

target_link_libraries(
        fortknox
        ${log-lib})
