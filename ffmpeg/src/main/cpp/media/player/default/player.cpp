//
// Created by Richie on 2020/12/23 0023.
//

#include "player.h"
#include "../../render/video/native/native_render.h"
#include "../../render/audio/opensl_render.h"


Player::Player(JNIEnv *env, jstring path, jobject surface) {
    m_v_decoder = new VideoDecoder(env, path);
    // 本地窗口播放
    m_v_render = new NativeRender(env, surface);
    m_v_decoder->SetRender(m_v_render);

    // 音频解码
    m_a_decoder = new AudioDecoder(env, path, false);
    m_a_render = new OpenSLRender();
    m_a_decoder->SetRender(m_a_render);
}

Player::~Player() = default;
// 此处不需要 delete 成员指针
// 在BaseDecoder中的线程已经使用智能指针，会自动释放

void Player::play() {
    if (m_v_decoder != nullptr) {
        m_v_decoder->Start();
        m_a_decoder->Start();
    }
}

void Player::pause() {
    if (m_v_decoder != nullptr) {
        m_v_decoder->Pause();
        m_a_decoder->Pause();
    }
}

void Player::release() {
    if (m_v_render != nullptr) {
        m_v_render->ReleaseRender();
        m_a_render->ReleaseRender();
    }
}
