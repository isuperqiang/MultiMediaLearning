package com.richie.multimedialearning.media;

/**
 * 解码状态回调接口
 *
 * @author Richie on 2020.12.29
 */
public interface IDecodeStateListener {
    void decoderPrepare(BaseDecoder decoder);

    void decoderReady(BaseDecoder decoder);

    void decoderRunning(BaseDecoder decoder);

    void decoderPause(BaseDecoder decoder);

    void decoderOneFrame(BaseDecoder decoder, Frame frame);

    void decoderFinish(BaseDecoder decoder);

    void decoderDestroy(BaseDecoder decoder);

    void decoderError(BaseDecoder decoder, String message);

}
