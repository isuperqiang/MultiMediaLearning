package com.richie.multimedialearning.media.extractor;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * 音频数据提取器
 *
 * @author Richie on 2020.12.30
 */
public class AudioExtractor implements IExtractor {
    private static final String TAG = "AudioExtractor";
    private final MMExtractor mMMExtractor;

    public AudioExtractor(String filePath) {
        mMMExtractor = new MMExtractor(filePath);
    }

    @Override
    public MediaFormat getFormat() {
        return mMMExtractor.getAudioFormat();
    }

    @Override
    public int readBuffer(ByteBuffer buffer) {
        return mMMExtractor.readBuffer(buffer);
    }

    @Override
    public long getSampleTimeStamp() {
        return mMMExtractor.getSampleTime();
    }

    @Override
    public int getSampleFlags() {
        return mMMExtractor.getSampleFlags();
    }

    @Override
    public long seek(long position) {
        return mMMExtractor.seek(position);
    }

    @Override
    public void setStartPosition(long position) {
        mMMExtractor.setStartPosition(position);
    }

    @Override
    public void stop() {
        mMMExtractor.stop();
    }
}
