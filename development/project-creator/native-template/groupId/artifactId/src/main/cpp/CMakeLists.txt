cmake_minimum_required(VERSION 3.22.1)

project(<NAME> LANGUAGES CXX)

add_library(<NAME>
        SHARED
        <NAME>.cpp)

set_property(TARGET <NAME>
        APPEND_STRING PROPERTY
        LINK_FLAGS
        " -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/version_scripts/libname.map.txt")

target_link_options(<NAME> PRIVATE "-Wl,-z,max-page-size=16384")
