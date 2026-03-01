cmake_minimum_required(VERSION 3.22.1)

project("pitchapp")

# Find the Oboe package provided by Prefab
find_package(oboe REQUIRED CONFIG)

add_library(${CMAKE_PROJECT_NAME} SHARED
        native-lib.cpp
        AudioEngine.cpp
        NoiseGate.cpp
        PitchDetector.cpp)

# Link the native library against Oboe, Android logging, and base Android libraries
target_link_libraries(${CMAKE_PROJECT_NAME}
        android
        log
        oboe::oboe)