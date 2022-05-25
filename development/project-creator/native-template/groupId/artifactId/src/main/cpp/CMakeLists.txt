cmake_minimum_required(VERSION 3.10.2)

project(<NAME> LANGUAGES CXX)

add_library(<NAME>
        SHARED
        <NAME>.cpp)

set_property(TARGET <NAME>
        APPEND_STRING PROPERTY
        LINK_FLAGS
        " -Wl,--version-script=${CMAKE_CURRENT_SOURCE_DIR}/version_scripts/libname.map.txt")