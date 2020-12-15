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
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 录音并编码生成 AAC 音频文件，这里采用 44.1kHz采样率、单声道、16位精度
 *
 * @author Richie on 2019.04.01
 */
public class AudioRecordEncoder {
    private static final String TAG = "AudioRecordEncoder";
    private static final boolean VERBOSE = false;
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
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    private AudioRecord mAudioRecord;
    private MediaCodec mMediaCodec;
    private volatile boolean mStopped;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    /**
     * 创建录音对象
     */
    public void createAudio() {
        // 获得缓冲区字节大小
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(AudioRecordEncoder.SAMPLE_RATE,
                AudioRecordEncoder.CHANNEL_CONFIG, AudioRecordEncoder.AUDIO_FORMAT);
        if (bufferSizeInBytes <= 0) {
            throw new RuntimeException("AudioRecord is not available, minBufferSize: " + bufferSizeInBytes);
        }
        mBufferSizeInBytes = bufferSizeInBytes;
        Log.i(TAG, "createAudioRecord minBufferSize: " + bufferSizeInBytes);

        AudioRecord audioRecord = new AudioRecord(AudioRecordEncoder.AUDIO_SOURCE, AudioRecordEncoder.SAMPLE_RATE,
                AudioRecordEncoder.CHANNEL_CONFIG, AudioRecordEncoder.AUDIO_FORMAT, bufferSizeInBytes);
        int state = audioRecord.getState();
        mAudioRecord = audioRecord;
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
        MediaCodec mediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec = mediaCodec;
    }

    public void start(final File outFile) {
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
        Log.d(TAG, "startAsync() called with: outFile = [" + outFile + "]");
        mStopped = false;
        MediaCodec mediaCodec = mMediaCodec;
        AudioRecord audioRecord = mAudioRecord;
        int bufferSizeInBytes = mBufferSizeInBytes;
        try (OutputStream fos = new FileOutputStream(outFile)) {
            mediaCodec.start();
            audioRecord.startRecording();

            ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
            MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
            final long timeoutUs = 10_000;
            while (!mStopped) {
                int inputBufIndex = mediaCodec.dequeueInputBuffer(timeoutUs);
                if (inputBufIndex >= 0) {
                    ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
                    inputBuffer.clear();
                    int remaining = inputBuffer.remaining();
                    int bufSize = Math.min(remaining, bufferSizeInBytes);
                    byte[] buffer = new byte[bufSize];
                    int readSize = audioRecord.read(buffer, 0, buffer.length);
                    if (readSize >= 0) {
                        inputBuffer.put(buffer);
                        inputBuffer.limit(buffer.length);
                        mediaCodec.queueInputBuffer(inputBufIndex, 0, readSize, 0, 0);
                        if (VERBOSE) {
                            Log.v(TAG, "queueInputBuffer index:" + inputBufIndex + ", size:" + readSize
                                    + ", remaining:" + remaining);
                        }
                    }

                    int outputBufferIndex = mediaCodec.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                    while (outputBufferIndex >= 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
                        outputBuffer.position(outBufferInfo.offset);
                        outputBuffer.limit(outBufferInfo.offset + outBufferInfo.size);
                        byte[] chunkAudio = new byte[outBufferInfo.size + 7];// 7 is ADTS size
                        addADTStoPacket(chunkAudio, chunkAudio.length);
                        outputBuffer.get(chunkAudio, 7, outBufferInfo.size);
                        outputBuffer.position(outBufferInfo.offset);
                        fos.write(chunkAudio);
                        mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
                        outputBufferIndex = mediaCodec.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                    }
                }
            }
        } finally {
            Log.i(TAG, "startAsync release");
            audioRecord.stop();
            audioRecord.release();
            mediaCodec.stop();
            mediaCodec.release();
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
