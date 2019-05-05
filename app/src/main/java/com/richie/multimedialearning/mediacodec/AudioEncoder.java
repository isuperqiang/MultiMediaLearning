package com.richie.multimedialearning.mediacodec;

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
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 录音并编码生成 AAC 音频文件，这里采用 44.1kHz采样率、单声道、16位精度
 *
 * @author Richie on 2019.04.01
 */
public class AudioEncoder {
    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
    // 输入源 麦克风
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    // 采样率 44100Hz，所有设备都支持
    private final static int SAMPLE_RATE = 44100;
    // 通道 单声道，所有设备都支持
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    // 精度 16 位，所有设备都支持
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // 通道数 单声道
    private static final int CHANNEL_COUNT = 1;
    // 比特率
    private static final int BIT_RATE = 96_000;
    private static final String TAG = "AudioEncoder";
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private volatile boolean mStopped;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

    /**
     * 创建录音对象
     */
    public void createAudio() {
        // 获得缓冲区字节大小
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(AudioEncoder.SAMPLE_RATE, AudioEncoder.CHANNEL_CONFIG, AudioEncoder.AUDIO_FORMAT);
        if (mBufferSizeInBytes <= 0) {
            throw new RuntimeException("AudioRecord is not available, minBufferSize: " + mBufferSizeInBytes);
        }
        Log.i(TAG, "createAudioRecord minBufferSize: " + mBufferSizeInBytes);

        mAudioRecord = new AudioRecord(AudioEncoder.AUDIO_SOURCE, AudioEncoder.SAMPLE_RATE, AudioEncoder.CHANNEL_CONFIG, AudioEncoder.AUDIO_FORMAT, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state: " + state + ", initialized: " + (state == AudioRecord.STATE_INITIALIZED));
    }

    public void createMediaCodec() throws IOException {
        MediaCodecInfo mediaCodecInfo = CodecUtils.selectCodec(MIMETYPE_AUDIO_AAC);
        if (mediaCodecInfo == null) {
            throw new RuntimeException(MIMETYPE_AUDIO_AAC + " encoder is not available");
        }
        Log.i(TAG, "createMediaCodec: mediaCodecInfo " + mediaCodecInfo.getName());

        MediaFormat format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        format.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);

        mMediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
    }

    public void start(final File outFile) throws IOException {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    startAsync(outFile);
                } catch (IOException e) {
                    Log.e(TAG, "startAsync: ", e);
                }
            }
        });
    }

    private void startAsync(File outFile) throws IOException {
        Log.d(TAG, "start() called with: outFile = [" + outFile + "]");
        mStopped = false;
        FileOutputStream fos = new FileOutputStream(outFile);
        mMediaCodec.start();
        mAudioRecord.startRecording();
        byte[] buffer = new byte[mBufferSizeInBytes];
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        try {
            while (!mStopped) {
                int readSize = mAudioRecord.read(buffer, 0, mBufferSizeInBytes);
                if (readSize > 0) {
                    int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                        inputBuffer.clear();
                        inputBuffer.put(buffer);
                        inputBuffer.limit(buffer.length);
                        mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, 0, 0);
                    }

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo, 0);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(bufferInfo.offset);
                        outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                        byte[] chunkAudio = new byte[bufferInfo.size + 7];// 7 is ADTS size
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
            mAudioRecord.stop();
            mAudioRecord.release();
            mMediaCodec.stop();
            mMediaCodec.release();
            fos.close();
        }
    }

    public void stop() {
        Log.d(TAG, "stop() called");
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
        int chanCfg = 1;  //CPE
        // fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
