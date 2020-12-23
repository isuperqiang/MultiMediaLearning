//
// Created by Richie on 2020/12/22.
//
// 定义解码器

#ifndef MULTIMEDIALEARNING_I_DECODER_H
#define MULTIMEDIALEARNING_I_DECODER_H

#include "i_decode_state_cb.h"

class IDecoder {
public:
    virtual void Start() = 0;

    virtual void Pause() = 0;

    virtual void Stop() = 0;

    virtual bool IsPlaying() = 0;

    virtual long GetDuration() = 0;

    virtual long GetCurrPos() = 0;

    virtual void SetStateReceiver(IDecodeStateCb *cb) = 0;
};

#endif //MULTIMEDIALEARNING_I_DECODER_H
