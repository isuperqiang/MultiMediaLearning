//
// Created by Richie on 2020/12/23 0023.
//

#ifndef MULTIMEDIALEARNING_TIMER_H
#define MULTIMEDIALEARNING_TIMER_H

#include "sys/time.h"

int64_t GetCurMsTime() {
    timeval tv;
    gettimeofday(&tv, nullptr);
    int64_t ts = (int64_t) tv.tv_sec * 1000 + tv.tv_usec / 1000;
    return ts;
}

#endif //MULTIMEDIALEARNING_TIMER_H
