//
// Created by Richie on 2020/12/23 0023.
//
// 普通播放器
#ifndef MULTIMEDIALEARNING_PLAYER_H
#define MULTIMEDIALEARNING_PLAYER_H

#include <jni.h>
#include "../../decoder/video/v_decoder.h"
#include "../../decoder/audio/audio_decoder.h"
#include "../../render/audio/audio_render.h"
#include "../../render/video/video_render.h"

class Player {
private:
    VideoDecoder *m_v_decoder;
    VideoRender *m_v_render;

    AudioDecoder *m_a_decoder;
    AudioRender *m_a_render;
public:
    Player(JNIEnv *env, jstring path, jobject surface);

    ~Player();

    void play();

    void pause();

    void release();
};

#endif //MULTIMEDIALEARNING_PLAYER_H
