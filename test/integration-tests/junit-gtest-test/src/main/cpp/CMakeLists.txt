cmake_minimum_required(VERSION 3.22.1)

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
        gtest_wrapper_test.cpp
        )

target_link_libraries(apptest
        PRIVATE
        adder
        googletest::gtest
        junit-gtest::junit-gtest
        )
target_link_options(adder
        PRIVATE
        "-Wl,-z,max-page-size=16384"
)
target_link_options(apptest
        PRIVATE
        "-Wl,-z,max-page-size=16384"
)
