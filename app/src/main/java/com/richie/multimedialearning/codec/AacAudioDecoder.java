package com.richie.multimedialearning.codec;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Richie on 2019.04.02
 */
public class AacAudioDecoder {
    private static final String TAG = "AacAudioDecoder";

    /**
     * 利用 MediaExtractor 和 MediaCodec 来提取编码后的音频数据并解压成音频源数据
     *
     * @param encodedFile
     * @param decodedFile
     * @throws IOException
     */
    public static void decodeAac2Pcm(File encodedFile, File decodedFile) throws IOException {
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
