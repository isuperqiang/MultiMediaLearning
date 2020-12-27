//
// Created by Richie on 2020/12/23 0023.
//
// 视频解码器

#ifndef MULTIMEDIALEARNING_V_DECODER_H
#define MULTIMEDIALEARNING_V_DECODER_H

#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include "../base_decoder.h"
#include "../../one_frame.h"
#include "../../render/video/video_render.h"

extern "C" {
#include <libswscale/swscale.h>
#include <libavutil/imgutils.h>
}

class VideoDecoder : public BaseDecoder {
private:
    const char *TAG = "VideoDecoder";

    //视频数据目标格式
    const AVPixelFormat DST_FORMAT = AV_PIX_FMT_RGBA;

    //存放YUV转换为RGB后的数据
    AVFrame *m_rgb_frame = nullptr;

    uint8_t *m_buf_for_rgb_frame = nullptr;

    //视频格式转换器
    SwsContext *m_sws_ctx = nullptr;

    //视频渲染器
    VideoRender *m_video_render = nullptr;

    //显示的目标宽
    int m_dst_w{};

    //显示的目标高
    int m_dst_h{};

    /**
     * 初始化渲染器
     */
    void InitRender(JNIEnv *env);

    /**
     * 初始化显示器
     * @param env
     */
    void InitBuffer();

    /**
     * 初始化视频数据转换器
     */
    void InitSws();

public:
    VideoDecoder(JNIEnv *env, jstring path, bool for_synthesizer = false);

    ~VideoDecoder();

    void SetRender(VideoRender *render);

protected:
    AVMediaType GetMediaType() override {
        return AVMEDIA_TYPE_VIDEO;
    }

    /**
     * 是否需要循环解码
     */
    bool NeedLoopDecode() override;

    /**
     * 准备解码环境
     * 注：在解码线程中回调
     * @param env 解码线程绑定的jni环境
     */
    void Prepare(JNIEnv *env) override;

    /**
     * 渲染
     * 注：在解码线程中回调
     * @param frame 解码RGBA数据
     */
    void Render(AVFrame *frame) override;

    /**
     * 释放回调
     */
    void Release(JNIEnv *env) override;

    const char *const LogSpec() override {
        return "VIDEO";
    };
};

#endif //MULTIMEDIALEARNING_V_DECODER_H
