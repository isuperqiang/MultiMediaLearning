//
// Created by Richie on 2020/5/5.
//
#ifndef _Included_RICHIE_ANDROID_LOG_UTIL_H
#define _Included_RICHIE_ANDROID_LOG_UTIL_H

#include  <android/log.h>

// ANDROID_LOG_VERBOSE, 2
/** Debug logging. Should typically be disabled for a release apk. */
// ANDROID_LOG_DEBUG, 3
/** Informational logging. Should typically be disabled for a release apk. */
// ANDROID_LOG_INFO, 4
/** Warning logging. For use with recoverable failures. */
// ANDROID_LOG_WARN, 5
/** Error logging. For use with unrecoverable failures. */
// ANDROID_LOG_ERROR, 6
/** Not logging. For release. */
// ANDROID_LOG_OFF, 7
#define LOG_LEVEL 3
#define TAG "ffmpeg-native"
#define LOGV(...) if(LOG_LEVEL <= ANDROID_LOG_VERBOSE) __android_log_print(ANDROID_LOG_VERBOSE, TAG ,__VA_ARGS__)
#define LOGD(...) if(LOG_LEVEL <= ANDROID_LOG_DEBUG) __android_log_print(ANDROID_LOG_DEBUG, TAG ,__VA_ARGS__)
#define LOGI(...) if(LOG_LEVEL <= ANDROID_LOG_INFO) __android_log_print(ANDROID_LOG_INFO, TAG ,__VA_ARGS__)
#define LOGW(...) if(LOG_LEVEL <= ANDROID_LOG_WARN) __android_log_print(ANDROID_LOG_WARN, TAG ,__VA_ARGS__)
#define LOGE(...) if(LOG_LEVEL <= ANDROID_LOG_ERROR) __android_log_print(ANDROID_LOG_ERROR, TAG ,__VA_ARGS__)

#endif // _Included_RICHIE_ANDROID_LOG_UTIL_H
