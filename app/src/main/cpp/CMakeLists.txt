cmake_minimum_required(VERSION 3.10.2)
project("ncnn_yolov8")

# ✅ NCNN 경로 설정
set(ncnn_LIB_DIR ${CMAKE_SOURCE_DIR}/libs/${ANDROID_ABI})
set(ncnn_INCLUDE_DIR ${CMAKE_SOURCE_DIR}/include)

# ✅ JNI용 네이티브 라이브러리 생성
add_library(
        ncnn_yolov8
        SHARED
        yolov8.cpp
)

# ✅ 안드로이드 로그 및 시스템 라이브러리
find_library(log-lib log)
find_library(android-lib android)

# ✅ NCNN 정적 라이브러리 연결
add_library(ncnn STATIC IMPORTED)
set_target_properties(ncnn PROPERTIES
        IMPORTED_LOCATION ${ncnn_LIB_DIR}/libncnn.a
        INTERFACE_INCLUDE_DIRECTORIES ${ncnn_INCLUDE_DIR}/ncnn
)

# ✅ 최종 타겟 링크 설정
target_link_libraries(
        ncnn_yolov8
        ncnn
        ${log-lib}
        ${android-lib}
)
