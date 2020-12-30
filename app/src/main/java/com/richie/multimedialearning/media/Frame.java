package com.richie.multimedialearning.media;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * 一帧的解码数据
 *
 * @author Richie on 2020.12.29
 */
public class Frame {
    public ByteBuffer mByteBuffer;
    private MediaCodec.BufferInfo mBufferInfo;

    public void setBufferInfo(MediaCodec.BufferInfo bufferInfo) {
        mBufferInfo.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
    }
}
