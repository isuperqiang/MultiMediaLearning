package com.richie.multimedialearning.mediacodec;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.opengl.EGLContext;
import android.opengl.GLES20;
import android.util.Log;
import android.view.Surface;

import com.richie.multimedialearning.utils.gles.EglCore;
import com.richie.multimedialearning.utils.gles.WindowSurface;
import com.richie.multimedialearning.utils.gles.program.TextureProgram;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Richie on 2020.08.22
 */
public class CameraSurfaceCodec {
    private static final String TAG = "CameraSurfaceCodec";
    // parameters for the encoder
    private static final String MIME_TYPE = "video/avc";    // H.264 Advanced Video Coding
    private static final int FRAME_RATE = 30;               // 30fps
    private static final int IFRAME_INTERVAL = 5;           // 5 seconds between I-frames
    private static final int VIDEO_BIT_RATE = 6000000;      // Mbps
    private static final String MIMETYPE_AUDIO_AAC = "audio/mp4a-latm";
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
    private static final int AUDIO_BIT_RATE = 64_000;
    private static final boolean VERBOSE = true;

    private MediaMuxer mMuxer;
    private boolean mMuxerAudioStarted;
    private boolean mMuxerVideoStarted;
    private boolean mMuxerStarted;
    private int mMuxerStoppedCount;
    private int mTexId;
    private float[] mMvpMatrix;
    private float[] mTexMatrix;
    private Surface mInputSurface;
    private int mRequestDrawFrame;
    private boolean mRequestRelease = true;
    private OnEncodeListener mOnEncodeListener;
    private boolean mEndOfStream;
    private boolean mEndOfAudio;
    private final Object mLockCodec = new Object();
    private final Object mLockDraw = new Object();
    private ExecutorService mExecutorsCodecVideo = Executors.newSingleThreadExecutor();
    private ExecutorService mExecutorsCodecAudio = Executors.newSingleThreadExecutor();
    private ExecutorService mExecutorsDrawer = Executors.newSingleThreadExecutor();

    public void setOnEncodeListener(OnEncodeListener onEncodeListener) {
        mOnEncodeListener = onEncodeListener;
    }

    public void create(final int width, final int height, final String outputPath) {
        Log.d(TAG, "create() width = [" + width + "], height = [" + height + "], outputPath = [" + outputPath + "]");
        synchronized (mLockCodec) {
            mEndOfAudio = false;
            mEndOfStream = false;
            mMuxerAudioStarted = false;
            mMuxerVideoStarted = false;
            mMuxerStarted = false;
            mMuxerStoppedCount = 0;
        }

        mExecutorsCodecVideo.execute(new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                // Set some properties.  Failing to specify some of these can cause the MediaCodec
                // configure() call to throw an unhelpful exception.
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, VIDEO_BIT_RATE);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                Log.d(TAG, "format: " + format);

                MediaCodec mediaCodec = null;
                try {
                    mediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
                    mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mInputSurface = mediaCodec.createInputSurface();
                    mediaCodec.start();

                    mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    Log.e(TAG, "create: ", e);
                }
                if (mediaCodec == null) {
                    Log.e(TAG, "create video media codec error");
                    return;
                }
                Log.i(TAG, "created video codec and muxer");
                if (mOnEncodeListener != null) {
                    mOnEncodeListener.onPrepare();
                }

                synchronized (mLockCodec) {
                    try {
                        mLockCodec.wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }

                Log.i(TAG, "start video codec");
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                int trackIndex = -1;
                while (true) {
                    synchronized (mLockCodec) {
                        if (mEndOfStream) {
                            Log.d(TAG, "sending EOS to video encoder");
                            mediaCodec.signalEndOfInputStream();
                            mEndOfStream = false;
                        }
                    }
                    int outBufIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 30_000);
                    if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) {
                            Log.d(TAG, "no output video available, spinning to await EOS");
                        }
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not expected for an encoder
                        outputBuffers = mediaCodec.getOutputBuffers();
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // should happen before receiving buffers, and should only happen once
                        if (mMuxerVideoStarted) {
                            throw new RuntimeException("format changed twice");
                        }
                        MediaFormat newFormat = mediaCodec.getOutputFormat();
                        Log.d(TAG, "encoder video output format changed: " + newFormat);

                        // now that we have the Magic Goodies, start the muxer
                        synchronized (mLockCodec) {
                            trackIndex = mMuxer.addTrack(newFormat);
                            mMuxerVideoStarted = true;
                            if (!mMuxerStarted && mMuxerAudioStarted) {
                                Log.i(TAG, "start muxer for video");
                                mMuxer.start();
                                mMuxerStarted = true;
                            }
                        }
                    } else if (outBufIndex < 0) {
                        Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + outBufIndex);
                    } else {
                        ByteBuffer encodedData = outputBuffers[outBufIndex];
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out and fed to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                            bufferInfo.size = 0;
                        }

                        if (bufferInfo.size != 0) {
                            if (!mMuxerStarted) {
                                mediaCodec.releaseOutputBuffer(outBufIndex, false);
                                continue;
                            }

                            encodedData.position(bufferInfo.offset);
                            encodedData.limit(bufferInfo.offset + bufferInfo.size);
                            bufferInfo.presentationTimeUs = getPresentationTimeUs(mVideoPrevOutputPTSUs);
                            mMuxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                            mVideoPrevOutputPTSUs = bufferInfo.presentationTimeUs;
                            if (VERBOSE) {
                                Log.d(TAG, "sent video " + bufferInfo.size + " bytes to muxer");
                            }
                        }
                        mediaCodec.releaseOutputBuffer(outBufIndex, false);
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "end of stream reached");
                            synchronized (mLockCodec) {
                                mMuxerStoppedCount++;
                            }
                            break;      // out of while
                        }
                    }
                }

                Log.i(TAG, "release video codec");
                mediaCodec.stop();
                mediaCodec.release();
                synchronized (mLockCodec) {
                    if (mMuxerStoppedCount == 2) {
                        if (mMuxer != null) {
                            Log.i(TAG, "run: release muxer");
                            mMuxer.stop();
                            mMuxer.release();
                            mMuxer = null;
                            if (mOnEncodeListener != null) {
                                mOnEncodeListener.onStop(outputPath);
                            }
                        }
                        mMuxerStoppedCount = 0;
                    }
                }
            }
        });

        mExecutorsCodecAudio.execute(new Runnable() {
            @Override
            public void run() {
                int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                if (minBufferSize <= 0) {
                    throw new RuntimeException("AudioRecord is not available, minBufferSize: " + minBufferSize);
                }
                Log.i(TAG, "createAudioRecord minBufferSize: " + minBufferSize);
                AudioRecord audioRecord = new AudioRecord(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT, minBufferSize);
                int state = audioRecord.getState();
                Log.i(TAG, "createAudio state: " + state + ", initialized: " + (state == AudioRecord.STATE_INITIALIZED));

                MediaCodecInfo mediaCodecInfo = CodecUtils.selectCodec(MIMETYPE_AUDIO_AAC);
                if (mediaCodecInfo == null) {
                    throw new RuntimeException(MIMETYPE_AUDIO_AAC + " encoder is not available");
                }
                Log.i(TAG, "createMediaCodec: audio mediaCodecInfo " + mediaCodecInfo.getName());

                MediaFormat format = MediaFormat.createAudioFormat(MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNEL_COUNT);
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
                format.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
                format.setInteger(MediaFormat.KEY_BIT_RATE, AUDIO_BIT_RATE);
                MediaCodec mediaCodec = null;
                try {
                    mediaCodec = MediaCodec.createEncoderByType(MIMETYPE_AUDIO_AAC);
                    mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mediaCodec.start();
                } catch (IOException e) {
                    Log.e(TAG, "run: ", e);
                }
                if (mediaCodec == null) {
                    Log.e(TAG, "create audio media codec error");
                    return;
                }

                Log.i(TAG, "start audio codec");
                ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
                ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
                MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
                final long timeoutUs = 30_000;
                int trackIndex = -1;
                audioRecord.startRecording();
                while (true) {
                    int inputBufIndex = mediaCodec.dequeueInputBuffer(timeoutUs);
                    boolean markEos = false;
                    synchronized (mLockCodec) {
                        if (mEndOfAudio) {
                            Log.d(TAG, "sending EOS to audio encoder");
                            mediaCodec.queueInputBuffer(inputBufIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            mEndOfAudio = false;
                            markEos = true;
                        }
                    }

                    if (!markEos && inputBufIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
                        inputBuffer.clear();
                        int remaining = inputBuffer.remaining();
                        int bufSize = Math.min(remaining, minBufferSize);
                        byte[] buffer = new byte[bufSize];
                        int readSize = audioRecord.read(buffer, 0, buffer.length);
                        if (readSize >= 0) {
                            inputBuffer.put(buffer);
                            inputBuffer.limit(buffer.length);
                            mediaCodec.queueInputBuffer(inputBufIndex, 0, readSize, 0, 0);
                            if (VERBOSE) {
                                Log.d(TAG, "send " + readSize + ", bytes to audio codec");
                            }
                        }
                    }

                    int outBufIndex = mediaCodec.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                    if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) {
                            Log.d(TAG, "no output audio available, spinning to await EOS");
                        }
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not expected for an encoder
                        outputBuffers = mediaCodec.getOutputBuffers();
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // should happen before receiving buffers, and should only happen once
                        if (mMuxerAudioStarted) {
                            throw new RuntimeException("audio format changed twice");
                        }
                        MediaFormat newFormat = mediaCodec.getOutputFormat();
                        Log.d(TAG, "encoder audio output format changed: " + newFormat);

                        // now that we have the Magic Goodies, start the muxer
                        synchronized (mLockCodec) {
                            trackIndex = mMuxer.addTrack(newFormat);
                            mMuxerAudioStarted = true;
                            if (!mMuxerStarted && mMuxerVideoStarted) {
                                Log.i(TAG, "start muxer for audio");
                                mMuxer.start();
                                mMuxerStarted = true;
                            }
                        }
                    } else if (outBufIndex < 0) {
                        Log.w(TAG, "unexpected result from encoder.dequeueOutputBuffer: " + outBufIndex);
                    } else {
                        ByteBuffer encodedData = outputBuffers[outBufIndex];
                        if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                            // The codec config data was pulled out and fed to the muxer when we got
                            // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                            Log.d(TAG, "ignoring BUFFER_FLAG_CODEC_CONFIG");
                            outBufferInfo.size = 0;
                        }

                        if (outBufferInfo.size != 0) {
                            if (!mMuxerStarted) {
                                mediaCodec.releaseOutputBuffer(outBufIndex, false);
                                continue;
                            }

                            encodedData.position(outBufferInfo.offset);
                            encodedData.limit(outBufferInfo.offset + outBufferInfo.size);
                            //outBufferInfo.presentationTimeUs = getPresentationTimeUs(mAudioPrevOutputPTSUs);
                            mMuxer.writeSampleData(trackIndex, encodedData, outBufferInfo);
                            //mAudioPrevOutputPTSUs = outBufferInfo.presentationTimeUs;
                            if (VERBOSE) {
                                Log.d(TAG, "sent audio " + outBufferInfo.size + " bytes to muxer");
                            }
                        }
                        mediaCodec.releaseOutputBuffer(outBufIndex, false);
                        if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "end of audio stream reached");
                            synchronized (mLockCodec) {
                                mMuxerStoppedCount++;
                            }
                            break;      // out of while
                        }
                    }
                }

                Log.i(TAG, "release audio codec");
                audioRecord.stop();
                audioRecord.release();
                mediaCodec.stop();
                mediaCodec.release();
                synchronized (mLockCodec) {
                    if (mMuxerStoppedCount == 2) {
                        if (mMuxer != null) {
                            Log.i(TAG, "run: release muxer");
                            mMuxer.stop();
                            mMuxer.release();
                            mMuxer = null;
                            if (mOnEncodeListener != null) {
                                mOnEncodeListener.onStop(outputPath);
                            }
                        }
                        mMuxerStoppedCount = 0;
                    }
                }
            }
        });
    }

    public void prepare(final EGLContext sharedContext) {
        Log.d(TAG, "prepare: ");
        mExecutorsDrawer.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "create egl");
                EglCore eglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
                WindowSurface windowSurface = new WindowSurface(eglCore, mInputSurface, true);
                windowSurface.makeCurrent();
                mInputSurface = null;
                TextureProgram textureProgram = TextureProgram.createTextureOES();

                synchronized (mLockDraw) {
                    mRequestRelease = false;
                    mRequestDrawFrame = 0;
                    try {
                        mLockDraw.wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }

                synchronized (mLockCodec) {
                    mLockCodec.notifyAll();
                }
                Log.i(TAG, "draw start");

                while (true) {
                    boolean localRequestDraw;
                    synchronized (mLockDraw) {
                        if (mRequestRelease) {
                            break;
                        }
                        localRequestDraw = mRequestDrawFrame > 0;
                        if (localRequestDraw) {
                            mRequestDrawFrame--;
                            if (VERBOSE) {
                                Log.d(TAG, "draw frame");
                            }
                        }
                    }
                    if (localRequestDraw) {
                        windowSurface.makeCurrent();
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                        textureProgram.drawFrame(mTexId, mTexMatrix, mMvpMatrix);
                        windowSurface.swapBuffers();
                    } else {
                        synchronized (mLockDraw) {
                            try {
                                mLockDraw.wait();
                            } catch (InterruptedException e) {
                                break;
                            }
                        }
                    }
                }

                Log.i(TAG, "release egl");
                textureProgram.release();
                windowSurface.release();
                eglCore.release();
            }
        });
    }

    public void draw(int texId, float[] texMatrix, float[] mvpMatrix) {
        synchronized (mLockDraw) {
            if (mRequestRelease) {
                mLockDraw.notifyAll();
                return;
            }
            mTexId = texId;
            mTexMatrix = texMatrix;
            mMvpMatrix = mvpMatrix;
            mRequestDrawFrame++;
            mLockDraw.notifyAll();
        }
    }

    public void release() {
        Log.d(TAG, "release: ");
        synchronized (mLockDraw) {
            mRequestRelease = true;
            mLockDraw.notifyAll();
        }
        synchronized (mLockCodec) {
            mEndOfStream = true;
            mEndOfAudio = true;
            mLockCodec.notifyAll();
        }
    }

    private long mAudioPrevOutputPTSUs = 0;
    private long mVideoPrevOutputPTSUs = 0;

    private long getPresentationTimeUs(long prev) {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prev) {
            result = (prev - result) + result;
        }
        return result;
    }

    public interface OnEncodeListener {
        /**
         * codec 准备完成
         */
        void onPrepare();

        /**
         * 录制结束
         *
         * @param path
         */
        void onStop(String path);
    }

}
