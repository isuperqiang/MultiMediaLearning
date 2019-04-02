package com.richie.multimedialearning.codec;

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Richie on 2019.04.02
 */
public class AudioDecoder {
    public static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
    private static final String TAG = "AudioDecoder";
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
    private MediaCodec mMediaCodec;

    public void createMediaCodec() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        try {
            mediaExtractor.setDataSource("");
            for (int i = 0, count = mediaExtractor.getTrackCount(); i < count; i++) {
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio/")) {
                    mediaExtractor.selectTrack(i);
                    mMediaCodec = MediaCodec.createDecoderByType(mime);
                    mMediaCodec.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "createMediaCodec: ", e);
        }

        if (mMediaCodec == null) {
            return;
        }

        long startTimeNano = System.nanoTime();
        mMediaCodec.start();
        ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
        while (true) {
            int inputBufferIndex = mMediaCodec.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                int readSize = mediaExtractor.readSampleData(inputBuffer, 0);
                if (readSize > 0) {
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, readSize, (System.nanoTime() - startTimeNano) / 1000, 0);
                    mediaExtractor.advance();
                } else {
                    break;
                }
            }

        }
    }

    public void decodeAac2Pcm(File encodedFile, File decodedFile) throws IOException {
        Log.d(TAG, "decodeAac2Pcm() called with: encodedFile = [" + encodedFile + "], decodedFile = [" + decodedFile + "]");
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(encodedFile.getAbsolutePath());
        MediaFormat mediaFormat = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                mediaFormat = format;
                break;
            }
        }
        if (mediaFormat == null) {
            Log.e(TAG, "not a valid file with audio track..");
            extractor.release();
            return;
        }

        OutputStream fosDecoder = new FileOutputStream(decodedFile);
        String mediaMime = mediaFormat.getString(MediaFormat.KEY_MIME);
        MediaCodec codec = MediaCodec.createDecoderByType(mediaMime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();
        ByteBuffer[] codecInputBuffers = codec.getInputBuffers();
        ByteBuffer[] codecOutputBuffers = codec.getOutputBuffers();
        final long kTimeOutUs = 5000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int totalRawSize = 0;

        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];
                        int sampleSize = extractor.readSampleData(dstBuf, 0);
                        if (sampleSize < 0) {
                            Log.i(TAG, "saw input EOS.");
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            long presentationTimeUs = extractor.getSampleTime();
                            codec.queueInputBuffer(inputBufIndex, 0, sampleSize, presentationTimeUs, 0);
                            extractor.advance();
                        }
                    }
                }

                int outputBufferIndex = codec.dequeueOutputBuffer(info, kTimeOutUs);
                if (outputBufferIndex >= 0) {
                    // Simply ignore codec config buffers.
                    if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.i(TAG, "audio encoder: codec config buffer");
                        codec.releaseOutputBuffer(outputBufferIndex, false);
                        continue;
                    }

                    if (info.size != 0) {
                        ByteBuffer outBuf = codecOutputBuffers[outputBufferIndex];
                        outBuf.position(info.offset);
                        outBuf.limit(info.offset + info.size);
                        byte[] data = new byte[info.size];
                        outBuf.get(data);
                        totalRawSize += data.length;
                        fosDecoder.write(data);
                    }

                    codec.releaseOutputBuffer(outputBufferIndex, false);

                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.i(TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    codecOutputBuffers = codec.getOutputBuffers();
                    Log.i(TAG, "output buffers have changed.");
                } else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat oformat = codec.getOutputFormat();
                    Log.i(TAG, "output format has changed to " + oformat);
                }
            }
        } finally {
            Log.i(TAG, "release totalRawSize: " + totalRawSize);
            fosDecoder.close();
            codec.stop();
            codec.release();
            extractor.release();
        }
    }

}
