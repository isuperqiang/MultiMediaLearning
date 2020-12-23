//
// Created by Richie on 2020/12/23 0023.
//

#ifndef MULTIMEDIALEARNING_LOGGER_H
#define MULTIMEDIALEARNING_LOGGER_H

#include <android/log.h>

#define LOG

#ifdef LOG
#define LOGD(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_DEBUG,TAG,FORMAT,##__VA_ARGS__);
#define LOGI(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_INFO,TAG,FORMAT,##__VA_ARGS__);
#define LOGW(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_WARN,TAG,FORMAT,##__VA_ARGS__);
#define LOGE(TAG, FORMAT, ...) __android_log_print(ANDROID_LOG_ERROR,TAG,FORMAT,##__VA_ARGS__);

#define LOG_INFO(TAG, SPEC, FMT, ...) LOGI(TAG, "[%s] " FMT, SPEC, ##__VA_ARGS__)
#define LOG_ERROR(TAG, SPEC, FMT, ...) LOGE(TAG, "[%s] " FMT, SPEC, ##__VA_ARGS__)
#else
#define LOGD(TAG, FORMAT,...);
#define LOGI(TAG, FORMAT,...);
#define LOGW(TAG, FORMAT,...) __android_log_print(ANDROID_LOG_WARN,TAG,FORMAT,##__VA_ARGS__);
#define LOGE(TAG, FORMAT,...) __android_log_print(ANDROID_LOG_ERROR,TAG,FORMAT,##__VA_ARGS__);
#endif
#endif //MULTIMEDIALEARNING_LOGGER_H
