cmake_minimum_required(VERSION 3.10.2)

project(junit-gtest-test LANGUAGES CXX)

find_package(googletest REQUIRED CONFIG)
find_package(junit-gtest REQUIRED CONFIG)

add_library(adder
        SHARED
        adder.cpp
        )

add_library(apptest
        SHARED
        app_test.cpp
        )

target_link_libraries(apptest
        PRIVATE
        adder
        googletest::gtest
        junit-gtest::junit-gtest
        )