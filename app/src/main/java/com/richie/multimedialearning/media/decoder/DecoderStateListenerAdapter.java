package com.richie.multimedialearning.media.decoder;

import com.richie.multimedialearning.media.Frame;

/**
 * @author Richie on 2020.12.30
 */
public abstract class DecoderStateListenerAdapter implements IDecodeStateListener {

    @Override
    public void decoderPrepare(BaseDecoder decoder) {

    }

    @Override
    public void decoderReady(BaseDecoder decoder) {

    }

    @Override
    public void decoderPause(BaseDecoder decoder) {

    }

    @Override
    public void decoderRunning(BaseDecoder decoder) {

    }

    @Override
    public void decoderOneFrame(BaseDecoder decoder, Frame frame) {

    }

    @Override
    public void decoderError(BaseDecoder decoder, String message) {

    }

    @Override
    public void decoderFinish(BaseDecoder decoder) {

    }

    @Override
    public void decoderDestroy(BaseDecoder decoder) {

    }
}

