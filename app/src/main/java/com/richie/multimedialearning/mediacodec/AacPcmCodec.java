package com.richie.multimedialearning.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Richie on 2019.04.03
 */
public final class AacPcmCodec {
    private static final String TAG = "AacPcmCodec";
    private final static String AUDIO_MIME = "audio/mp4a-latm";
    private final static long AUDIO_BYTES_PER_SAMPLE = 44100 * 1 * 16 / 8;

    /**
     * AAC 格式解码成 PCM 数据
     *
     * @param aacFile
     * @param pcmFile
     * @throws IOException
     */
    public static void decodeAacToPcm(File aacFile, File pcmFile) throws IOException {
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(aacFile.getAbsolutePath());
        MediaFormat mediaFormat = null;
        for (int i = 0, count = extractor.getTrackCount(); i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                extractor.selectTrack(i);
                mediaFormat = format;
                break;
            }
        }
        if (mediaFormat == null) {
            Log.e(TAG, "Invalid file with audio track.");
            extractor.release();
            return;
        }

        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        Log.i(TAG, "decodeAacToPcm: mimeType: " + mime);
        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(mediaFormat, null, null, 0);
        codec.start();
        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        ByteBuffer[] outputBuffers = codec.getOutputBuffers();
        MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
        final long timeoutUs = 10_000;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        byte[] outputBytes = null;
        try (OutputStream fosAudio = new FileOutputStream(pcmFile)) {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(timeoutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
                        int sampleSize = extractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            Log.i(TAG, "saw input EOS.");
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        } else {
                            codec.queueInputBuffer(inputBufIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }

                int outputBufIndex = codec.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                if (outputBufIndex >= 0) {
                    // Simply ignore codec config buffers.
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.i(TAG, "audio encoder: codec config buffer");
                        codec.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }
                    if (outBufferInfo.size > 0) {
                        ByteBuffer outputBuffer = outputBuffers[outputBufIndex];
                        outputBuffer.position(outBufferInfo.offset);
                        outputBuffer.limit(outBufferInfo.offset + outBufferInfo.size);
                        if (outputBytes == null || outputBytes.length < outBufferInfo.size) {
                            outputBytes = new byte[outBufferInfo.size];
                        }
                        outputBuffer.get(outputBytes);
                        fosAudio.write(outputBytes);
                    }
                    codec.releaseOutputBuffer(outputBufIndex, false);
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.i(TAG, "saw output EOS.");
                        sawOutputEOS = true;
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = codec.getOutputBuffers();
                    Log.i(TAG, "output buffers have changed.");
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "output format has changed to " + codec.getOutputFormat());
                }
            }
        } finally {
            Log.i(TAG, "decodeAacToPcm finish " + pcmFile.getAbsolutePath());
            codec.stop();
            codec.release();
            extractor.release();
        }
    }

    /**
     * PCM 数据编码为 AAC 格式
     *
     * @param inPcmFile
     * @param outAacFile
     * @throws IOException
     */
    public static void encodePcmToAac(File inPcmFile, File outAacFile) throws IOException {
        MediaCodec audioEncoder = createAudioEncoder();
        try (InputStream fisAudio = new FileInputStream(inPcmFile);
             OutputStream fosAudio = new FileOutputStream(outAacFile)) {
            audioEncoder.start();
            boolean sawInputEOS = false;
            boolean sawOutputEOS = false;
            long presentationTimeUs = 0;
            byte[] inputBytes = null;
            int sumReadInputSize = 0;
            long outputPresentationTimeUs = 0;
            final int timeoutUs = 10_000;
            MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = audioEncoder.getInputBuffers();
            ByteBuffer[] outputBuffers = audioEncoder.getOutputBuffers();
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = audioEncoder.dequeueInputBuffer(timeoutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
                        inputBuffer.clear();
                        int bufferSize = inputBuffer.remaining();
                        if (inputBytes == null) {
                            inputBytes = new byte[bufferSize];
                        }
                        int readInputSize = fisAudio.read(inputBytes);
                        if (readInputSize < 0) {
                            audioEncoder.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            sawInputEOS = true;
                            Log.i(TAG, "saw input EOS.");
                        } else {
                            inputBuffer.put(inputBytes, 0, readInputSize);
                            sumReadInputSize += readInputSize;
                            audioEncoder.queueInputBuffer(inputBufIndex, 0, readInputSize, presentationTimeUs, 0);
                            presentationTimeUs = (long) (1_000_000 * ((float) sumReadInputSize / AUDIO_BYTES_PER_SAMPLE));
                        }
                    }
                }

                int outputBufIndex = audioEncoder.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                if (outputBufIndex >= 0) {
                    // Simply ignore codec config buffers.
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        Log.i(TAG, "audio encoder: codec config buffer");
                        audioEncoder.releaseOutputBuffer(outputBufIndex, false);
                        continue;
                    }
                    if (outBufferInfo.size > 0) {
                        ByteBuffer outBuffer = outputBuffers[outputBufIndex];
                        outBuffer.position(outBufferInfo.offset);
                        outBuffer.limit(outBufferInfo.offset + outBufferInfo.size);
                        if (outputPresentationTimeUs <= outBufferInfo.presentationTimeUs) {
                            outputPresentationTimeUs = outBufferInfo.presentationTimeUs;
                            int outBufSize = outBufferInfo.size;
                            int outPacketSize = outBufSize + 7;
                            outBuffer.position(outBufferInfo.offset);
                            outBuffer.limit(outBufferInfo.offset + outBufSize);
                            byte[] outData = new byte[outPacketSize];
                            addADTStoPacket(outData, outPacketSize);
                            outBuffer.get(outData, 7, outBufSize);
                            fosAudio.write(outData, 0, outData.length);
                        }
                    }
                    audioEncoder.releaseOutputBuffer(outputBufIndex, false);
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        sawOutputEOS = true;
                        Log.i(TAG, "saw output EOS.");
                    }
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    outputBuffers = audioEncoder.getOutputBuffers();
                    Log.i(TAG, "output buffers have changed.");
                } else if (outputBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    Log.i(TAG, "output format has changed to " + audioEncoder.getOutputFormat());
                }
            }
        } finally {
            Log.i(TAG, "encodePcmToAac: finish " + outAacFile.getAbsolutePath());
            audioEncoder.release();
        }
    }

    private static MediaCodec createAudioEncoder() throws IOException {
        MediaCodec codec = MediaCodec.createEncoderByType(AUDIO_MIME);
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, AUDIO_MIME);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return codec;
    }

    /**
     * Add ADTS header at the beginning of each and every AAC packet.
     * This is needed as MediaCodec encoder generates a packet of raw
     * AAC data.
     * <p>
     * Note the packetLen must count in the ADTS header itself.
     * <p>
     * see https://blog.csdn.net/bsplover/article/details/7426476,
     * Sample rate and channel count are variables.
     **/
    private static void addADTStoPacket(byte[] packet, int packetLen) {
        int profile = 2;  //AAC LC
        //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
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
