package com.richie.multimedialearning.media.decoder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;

import com.richie.multimedialearning.media.BaseDecoder;
import com.richie.multimedialearning.media.IExtractor;
import com.richie.multimedialearning.media.extractor.AudioExtractor;

import java.nio.ByteBuffer;

/**
 * @author Richie on 2020.12.30
 */
public class AudioDecoder extends BaseDecoder {
    private static final String TAG = "AudioDecoder";
    private int mSampleRate = -1;
    private int mChannels = -1;
    private int mEncodingPcmBit = AudioFormat.ENCODING_PCM_16BIT;
    private AudioTrack mAudioTrack;
    private short[] mAudioOutTempBuf;

    public AudioDecoder(String filePath) {
        super(filePath);
    }

    @Override
    protected boolean check() {
        return true;
    }

    @Override
    protected IExtractor initExtractor(String filePath) {
        return new AudioExtractor(filePath);
    }

    @Override
    protected void initSpecParams(MediaFormat mediaFormat) {
        Log.d(TAG, "initSpecParams: ");
        try {
            mChannels = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            mSampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            if (mediaFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                mEncodingPcmBit = mediaFormat.getInteger(MediaFormat.KEY_PCM_ENCODING);
            } else {
                mEncodingPcmBit = AudioFormat.ENCODING_PCM_16BIT;
            }
        } catch (Exception e) {
            Log.w(TAG, "initSpecParams: ", e);
        }
    }

    @Override
    protected boolean configCodec(MediaCodec mediaCodec, MediaFormat mediaFormat) {
        mediaCodec.configure(mediaFormat, null, null, 0);
        return true;
    }

    @Override
    protected boolean initRender() {
        Log.d(TAG, "initRender: ");
        int channel = mChannels == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO;
        int minBufferSize = AudioTrack.getMinBufferSize(mSampleRate, channel, mEncodingPcmBit);
        mAudioOutTempBuf = new short[minBufferSize / 2];
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mSampleRate, channel, mEncodingPcmBit, minBufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.play();
        return true;
    }

    @Override
    protected void render(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {
        int length = bufferInfo.size / 2;
        if (mAudioOutTempBuf.length < length) {
            mAudioOutTempBuf = new short[length];
        }
        outputBuffer.position(0);
        outputBuffer.asShortBuffer().get(mAudioOutTempBuf, 0, length);
        mAudioTrack.write(mAudioOutTempBuf, 0, length);
    }

    @Override
    protected void doneDecode() {
        mAudioTrack.stop();
        mAudioTrack.release();
    }
}
