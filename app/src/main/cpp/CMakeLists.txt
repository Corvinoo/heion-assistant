cmake_minimum_required(VERSION 3.22.1)

project("phonemizer_jni")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# =========================
# Existing imported library
# =========================

add_library(ttsespeak SHARED IMPORTED)

set_target_properties(ttsespeak PROPERTIES
        IMPORTED_LOCATION
        ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libttsespeak.so
)

# =========================
# fastText sources
# =========================

set(FASTTEXT_SRC_DIR
        ${CMAKE_CURRENT_SOURCE_DIR}/fastText/src)

file(GLOB FASTTEXT_SOURCES
        ${FASTTEXT_SRC_DIR}/*.cc)

# fastText CLI entrypoint must NOT be compiled on Android
list(REMOVE_ITEM FASTTEXT_SOURCES
        ${FASTTEXT_SRC_DIR}/main.cc)

# =========================
# Main JNI library
# =========================

add_library(${PROJECT_NAME} SHARED
        phonemizer_jni.cpp
        jnifasttextwrapper.cpp
        ${FASTTEXT_SOURCES}
)

# =========================
# Include directories
# =========================

target_include_directories(${PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/include
        ${FASTTEXT_SRC_DIR}
)

# =========================
# Android system libraries
# =========================

find_library(log-lib log)
find_library(android-lib android)

# =========================
# Linking
# =========================

target_link_libraries(${PROJECT_NAME}
        ttsespeak
        ${android-lib}
        ${log-lib}
)