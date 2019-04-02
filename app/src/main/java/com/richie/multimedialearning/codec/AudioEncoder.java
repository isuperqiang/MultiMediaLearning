package com.richie.multimedialearning.codec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Richie on 2019.04.01
 */
public class AudioEncoder {
    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
    // 音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    // 44100是目前的标准
    private final static int AUDIO_SAMPLE_RATE = 44100;
    // 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    // 编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 比特率
    private static final int BIT_RATE = 64000;
    private static final String TAG = "AudioEncoder";
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private volatile boolean mStopped;

    /**
     * 创建录音对象
     */
    public void createAudio() {
        // 获得缓冲区字节大小
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(AudioEncoder.AUDIO_SAMPLE_RATE, AudioEncoder.AUDIO_CHANNEL, AudioEncoder.AUDIO_ENCODING);
        if (mBufferSizeInBytes == AudioRecord.ERROR_BAD_VALUE || mBufferSizeInBytes == AudioRecord.ERROR) {
            throw new RuntimeException("AudioRecord is not available");
        }

        mAudioRecord = new AudioRecord(AudioEncoder.AUDIO_INPUT, AudioEncoder.AUDIO_SAMPLE_RATE, AudioEncoder.AUDIO_CHANNEL, AudioEncoder.AUDIO_ENCODING, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state:" + state + ", initialized:" + (state == AudioRecord.STATE_INITIALIZED));
    }

    public void createMediaCodec() {
        MediaCodecInfo mediaCodecInfo = CodecUtils.selectCodec(MIMETYPE_AUDIO_AAC);
        if (mediaCodecInfo != null) {
            Log.i(TAG, "createMediaCodec: mediaCodecInfo " + mediaCodecInfo.getName());
        } else {
            throw new RuntimeException("encoder is not available");
        }

        MediaFormat format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, AUDIO_SAMPLE_RATE, 1);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, mBufferSizeInBytes * 2);

        try {
            mMediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
            mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            Log.e(TAG, "createCodec: ", e);
        }
    }

    public void start(File outFile) throws IOException {
        Log.d(TAG, "start() called with: outFile = [" + outFile + "]");
        mStopped = false;
        OutputStream fos = new FileOutputStream(outFile);
        mMediaCodec.start();
        mAudioRecord.startRecording();
        byte[] buffer = new byte[mBufferSizeInBytes];
        long startNanoTime = System.nanoTime();
        try {
            while (!mStopped) {
                int readSize = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize && AudioRecord.ERROR_BAD_VALUE != readSize
                        && AudioRecord.ERROR_DEAD_OBJECT != readSize && AudioRecord.ERROR != readSize) {
                    ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(buffer);
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, (System.nanoTime() - startNanoTime) / 1000, 0);
                    }

                    ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] chunkAudio = new byte[bufferInfo.size + 7];
                        addADTStoPacket(chunkAudio, chunkAudio.length);
                        outputBuffer.get(chunkAudio, 7, bufferInfo.size);
                        outputBuffer.position(bufferInfo.offset);
                        fos.write(chunkAudio);
                        mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    }
                } else {
                    Log.w(TAG, "read audio buffer error:" + readSize);
                    break;
                }
            }
        } finally {
            Log.i(TAG, "released");
            fos.close();
            mAudioRecord.stop();
            mMediaCodec.stop();
            mMediaCodec.release();
            mAudioRecord.release();
        }
    }

    public void stop() {
        mStopped = true;
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet.
     * This is needed as MediaCodec encoder generates a packet of raw
     * AAC data.
     * <p>
     * Note the packetLen must count in the ADTS header itself.
     */
    private void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        int freqIdx = 4;  //44.1KHz
        int chanCfg = 2;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    // http://lastwarmth.win/2016/10/22/live-audio/

}
