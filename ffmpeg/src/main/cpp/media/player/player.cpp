//
// Created by Richie on 2020/12/23 0023.
//

#include "player.h"
#include "../render/video/native/native_render.h"


Player::Player(JNIEnv *env, jstring path, jobject surface) {
    m_v_decoder = new VideoDecoder(env, path);
    // 本地窗口播放
    m_v_render = new NativeRender(env, surface);
    m_v_decoder->SetRender(m_v_render);
}

void Player::play() {
    if (m_v_decoder != nullptr) {
        m_v_decoder->Start();
    }
}

void Player::pause() {
    if (m_v_decoder != nullptr) {
        m_v_decoder->Pause();
    }
}

void Player::release() {
    if (m_v_render != nullptr) {
        m_v_render->ReleaseRender();
    }
}
