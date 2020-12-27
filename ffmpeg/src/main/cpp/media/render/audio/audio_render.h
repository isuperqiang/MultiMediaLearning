//
// Created by Richie on 2020/12/27.
//

#ifndef MULTIMEDIALEARNING_AUDIO_RENDER_H
#define MULTIMEDIALEARNING_AUDIO_RENDER_H


#include <cstdint>

class AudioRender {
public:
    virtual void InitRender() = 0;

    virtual void Render(uint8_t *pcm, int size) = 0;

    virtual void ReleaseRender() = 0;

    virtual ~AudioRender() {}
};


#endif //MULTIMEDIALEARNING_AUDIO_RENDER_H
