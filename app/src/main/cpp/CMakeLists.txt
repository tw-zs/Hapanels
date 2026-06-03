cmake_minimum_required(VERSION 3.22.1)

project(hapanels_shellyinput)

add_library(shellyinput SHARED shellyinput.cpp)

find_library(log-lib log)

target_link_libraries(shellyinput ${log-lib})
