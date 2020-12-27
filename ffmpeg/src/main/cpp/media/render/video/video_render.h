//
// Created by Richie on 2020/12/23 0023.
//

#ifndef MULTIMEDIALEARNING_VIDEO_RENDER_H
#define MULTIMEDIALEARNING_VIDEO_RENDER_H

#include <jni.h>
#include <stdint.h>
#include "../../one_frame.h"

class VideoRender {
public:
    VideoRender() {

    }

    ~VideoRender() {

    }

    virtual void InitRender(JNIEnv *env, int video_width, int video_height, int *dst_size) = 0;

    virtual void Render(OneFrame *oneFrame) = 0;

    virtual void ReleaseRender(JNIEnv *env) = 0;
};

#endif //MULTIMEDIALEARNING_VIDEO_RENDER_H
