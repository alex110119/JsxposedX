cmake_minimum_required(VERSION 3.22.1)
project(so_analysis)

include(FetchContent)
include(${CMAKE_CURRENT_SOURCE_DIR}/cmake/patch_keystone_python3.cmake)

set(CAPSTONE_ARCHITECTURE_DEFAULT OFF)
set(CAPSTONE_ARM_SUPPORT ON)
set(CAPSTONE_ARM64_SUPPORT ON)
set(CAPSTONE_X86_SUPPORT ON)
set(CAPSTONE_MIPS_SUPPORT OFF)
set(CAPSTONE_PPC_SUPPORT OFF)
set(CAPSTONE_SPARC_SUPPORT OFF)
set(CAPSTONE_SYSZ_SUPPORT OFF)
set(CAPSTONE_XCORE_SUPPORT OFF)
set(CAPSTONE_M68K_SUPPORT OFF)
set(CAPSTONE_TMS320C64X_SUPPORT OFF)
set(CAPSTONE_M680X_SUPPORT OFF)
set(CAPSTONE_EVM_SUPPORT OFF)
set(CAPSTONE_BUILD_STATIC ON)
set(CAPSTONE_BUILD_SHARED OFF)
set(CAPSTONE_BUILD_TESTS OFF)
set(CAPSTONE_BUILD_CSTOOL OFF)

FetchContent_Declare(
    capstone
    GIT_REPOSITORY https://github.com/capstone-engine/capstone.git
    GIT_TAG        5.0.1
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(capstone)

find_package(Python3 COMPONENTS Interpreter)

# Windows 上 PATH 里可能存在 C:\Windows\System32\python 这类“应用执行别名”，
# CMake 会选中它但无法执行，导致 find_package 失败；随后 keystone 的
# llvm/CMakeLists.txt 会在 if(PYTHON_VERSION_STRING VERSION_LESS 2.7) 处语法报错。
# 这里补一次容错探测：优先用 py 启动器，再试 python3 / python，并校验确实是 3.x。
if(NOT Python3_Interpreter_FOUND)
    foreach(_jsx_py_name py python3 python)
        unset(JSX_PYTHON_CANDIDATE CACHE)
        find_program(JSX_PYTHON_CANDIDATE NAMES ${_jsx_py_name})
        if(JSX_PYTHON_CANDIDATE)
            if(_jsx_py_name STREQUAL "py")
                set(_jsx_py_probe "${JSX_PYTHON_CANDIDATE}" -3 -c "import sys;print(sys.version_info[0])")
            else()
                set(_jsx_py_probe "${JSX_PYTHON_CANDIDATE}" -c "import sys;print(sys.version_info[0])")
            endif()

            execute_process(
                COMMAND ${_jsx_py_probe}
                OUTPUT_VARIABLE _jsx_py_major
                OUTPUT_STRIP_TRAILING_WHITESPACE
                RESULT_VARIABLE _jsx_py_result
                ERROR_QUIET)

            if(_jsx_py_result EQUAL 0 AND _jsx_py_major STREQUAL "3")
                set(Python3_EXECUTABLE "${JSX_PYTHON_CANDIDATE}" CACHE FILEPATH "" FORCE)
                set(Python3_Interpreter_FOUND TRUE)
                message(STATUS "JsxposedX: 回退使用 Python3 解释器 ${JSX_PYTHON_CANDIDATE}")
                break()
            endif()
        endif()
    endforeach()
    unset(JSX_PYTHON_CANDIDATE CACHE)
endif()

set(KEYSTONE_BUILD_STATIC_RUNTIME ON CACHE BOOL "" FORCE)
set(BUILD_LIBS_ONLY ON CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLVM_TARGETS_TO_BUILD "ARM;AArch64" CACHE STRING "" FORCE)
if(Python3_Interpreter_FOUND)
    set(PYTHON_EXECUTABLE "${Python3_EXECUTABLE}" CACHE FILEPATH "" FORCE)
else()
    # 兜底：keystone 只编译静态库，不实际执行 Python 脚本；
    # 给一个合法的版本串即可让 LLVM 的 if() 判断通过，避免配置阶段硬失败。
    message(WARNING "JsxposedX: 未找到可用的 Python3，使用占位版本串继续配置 keystone")
    set(PYTHON_VERSION_STRING "3.11.0" CACHE STRING "" FORCE)
    set(PYTHON_VERSION_MAJOR "3" CACHE STRING "" FORCE)
    set(PYTHON_VERSION_MINOR "11" CACHE STRING "" FORCE)
endif()

FetchContent_Declare(
    keystone
    GIT_REPOSITORY https://github.com/keystone-engine/keystone.git
    GIT_TAG        0.9.2
    GIT_SHALLOW    TRUE
)

FetchContent_GetProperties(keystone)
if(NOT keystone_POPULATED)
    FetchContent_Populate(keystone)
    patch_keystone_python3(${keystone_SOURCE_DIR})
    add_subdirectory(${keystone_SOURCE_DIR} ${keystone_BINARY_DIR} EXCLUDE_FROM_ALL)
endif()

set(LIEF_PREBUILT_DIR ${CMAKE_SOURCE_DIR}/../jniLibs/lief/${ANDROID_ABI})
set(LIEF_LIB ${LIEF_PREBUILT_DIR}/libLIEF.a)

if(EXISTS ${LIEF_LIB})
    add_library(LIEF STATIC IMPORTED)
    set_target_properties(LIEF PROPERTIES
        IMPORTED_LOCATION ${LIEF_LIB}
        INTERFACE_INCLUDE_DIRECTORIES ${CMAKE_SOURCE_DIR}/../jniLibs/lief/include
    )

    add_library(so_analysis SHARED
        so_analysis_jni.cpp
    )

    add_library(memory_tool SHARED
        memory_tool_breakpoint.cpp
        memory_tool_jni.cpp
        memory_tool_engine.cpp
        memory_tool_instruction.cpp
        memory_tool_protocol.cpp
        memory_tool_reader.cpp
        memory_tool_regions.cpp
        memory_tool_scanner.cpp
        memory_tool_session.cpp
        memory_tool_utils.cpp
        memory_tool_value.cpp
    )

    target_include_directories(so_analysis PRIVATE
        ${CMAKE_SOURCE_DIR}/../jniLibs/lief/include
        ${capstone_SOURCE_DIR}/include
    )

    target_include_directories(memory_tool PRIVATE
        ${capstone_SOURCE_DIR}/include
        ${keystone_SOURCE_DIR}/include
    )

    target_link_libraries(so_analysis
        LIEF
        capstone
        android
        log
        z
    )

    target_link_libraries(memory_tool
        keystone
        capstone
        android
        log
    )

    foreach(native_target so_analysis memory_tool)
        target_compile_options(${native_target} PRIVATE
            -fexceptions
            -frtti
            -std=c++17
        )
    endforeach()
else()
    message(STATUS "LIEF not found for ${ANDROID_ABI}, skipping so_analysis and memory_tool")
endif()
