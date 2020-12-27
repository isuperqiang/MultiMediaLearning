cmake_minimum_required(VERSION 3.4.1)

# 支持gnu++11
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=gnu++11")

# 1. 定义so库和头文件所在目录，方面后面使用
set(ffmpeg_lib_dir ${CMAKE_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})
set(ffmpeg_head_dir ${CMAKE_SOURCE_DIR}/ffmpeg/include)

# 2. 添加头文件目录
include_directories(${ffmpeg_head_dir})

# 3. 添加ffmpeg相关的so库
add_library(avutil
        SHARED
        IMPORTED)
set_target_properties(avutil
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavutil.so)

add_library(swresample
        SHARED
        IMPORTED)
set_target_properties(swresample
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswresample.so)

add_library(avcodec
        SHARED
        IMPORTED)
set_target_properties(avcodec
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavcodec.so)

add_library(avfilter
        SHARED
        IMPORTED)
set_target_properties(avfilter
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavfilter.so)

add_library(swscale
        SHARED
        IMPORTED)
set_target_properties(swscale
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libswscale.so)

add_library(avformat
        SHARED
        IMPORTED)
set_target_properties(avformat
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavformat.so)

add_library(avdevice
        SHARED
        IMPORTED)
set_target_properties(avdevice
        PROPERTIES IMPORTED_LOCATION
        ${ffmpeg_lib_dir}/libavdevice.so)

# 查找代码中使用到的系统库
find_library( # Sets the name of the path variable.
        log-lib

        # Specifies the name of the NDK library that
        # you want CMake to locate.
        log)

# 配置目标so库编译信息
add_library( # Sets the name of the library.
        ffmpegnative

        # Sets the library as a shared library.
        SHARED

        # Provides a relative path to your source file(s).
        ffmpeg_native.cpp
        log_util.h
        media/decoder/decode_state.h
        media/decoder/i_decoder.h
        media/decoder/base_decoder.h
        media/decoder/base_decoder.cpp
        media/decoder/i_decode_state_cb.h
        media/decoder/i_decode_state_cb.cpp
        media/decoder/video/v_decoder.h
        media/decoder/video/v_decoder.cpp
        media/decoder/video/v_decoder.cpp
        media/decoder/audio/audio_decoder.h
        media/decoder/audio/audio_decoder.cpp
        media/render/video/video_render.h
        media/render/video/native/native_render.h
        media/render/video/native/native_render.cpp
        media/render/audio/audio_render.h
        media/render/audio/opensl_render.h
        media/render/audio/opensl_render.cpp
        media/player/default/player.h
        media/player/default/player.cpp
        media/one_frame.h
        media/const.h
        utils/logger.h
        utils/timer.h
        )

# 指定编译目标库时，cmake要链接的库
target_link_libraries(

        # 指定目标库，ffmpegnative 是在上面 add_library 中配置的目标库
        ffmpegnative

        # 4. 连接 FFmpeg 相关的库
        avutil
        swresample
        avcodec
        avfilter
        swscale
        avformat
        avdevice

        -landroid
        OpenSLES
        -lEGL
        -lGLESv2

        # Links the target library to the log library
        # included in the NDK.
        ${log-lib})
