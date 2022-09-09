cmake_minimum_required(VERSION 3.18.1)
project("androidx.graphics.path")

add_library(
        androidx.graphics.path
        SHARED
        Conic.cpp
        PathIterator.cpp
        pathway.cpp
)

find_library(
        log-lib
        log
)

target_link_libraries(
        androidx.graphics.path
        ${log-lib}
)
