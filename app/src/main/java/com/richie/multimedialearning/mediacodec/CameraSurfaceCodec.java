package com.richie.multimedialearning.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
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
    private static final int ENC_BIT_RATE = 6000000;      // Mbps
    private static final boolean VERBOSE = false;

    // encoder / muxer state
    private MediaCodec mEncoder;
    //private CodecInputSurface mInputSurface;
    private MediaMuxer mMuxer;
    private int mTrackIndex = -1;
    private boolean mMuxerStarted;
    // allocate one of these up front so we don't need to do it every time
    private Surface mInputSurface;
    private TextureProgram mTextureProgram;
    private EglCore mEglCore;
    private WindowSurface mWindowSurface;
    private int mTexId;
    private float[] mMvpMatrix;
    private float[] mTexMatrix;
    private int mRequestDrawFrame;
    private boolean mRequestRelease = true;
    private OnEncodeListener mOnEncodeListener;
    private boolean mEndOfStream;
    private final Object mLockCodec = new Object();
    private final Object mLockDraw = new Object();
    private ExecutorService mExecutorsCodec = Executors.newSingleThreadExecutor();
    private ExecutorService mExecutorsDrawer = Executors.newSingleThreadExecutor();

    public void setOnEncodeListener(OnEncodeListener onEncodeListener) {
        mOnEncodeListener = onEncodeListener;
    }

    public void create(final int width, final int height, final String outputPath) {
        Log.d(TAG, "create() width = [" + width + "], height = [" + height + "], outputPath = [" + outputPath + "]");
        mExecutorsCodec.execute(new Runnable() {
            @Override
            public void run() {
                MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                MediaFormat format = MediaFormat.createVideoFormat(MIME_TYPE, width, height);
                // Set some properties.  Failing to specify some of these can cause the MediaCodec
                // configure() call to throw an unhelpful exception.
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
                format.setInteger(MediaFormat.KEY_BIT_RATE, ENC_BIT_RATE);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, IFRAME_INTERVAL);
                Log.d(TAG, "format: " + format);

                try {
                    mEncoder = MediaCodec.createEncoderByType(MIME_TYPE);
                    mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                    mInputSurface = mEncoder.createInputSurface();
                    mEncoder.start();

                    mMuxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
                } catch (IOException e) {
                    Log.e(TAG, "create: ", e);
                }
                mMuxerStarted = false;
                Log.i(TAG, "create codec and muxer");
                if (mOnEncodeListener != null) {
                    mOnEncodeListener.onPrepare();
                }

                synchronized (mLockCodec) {
                    mEndOfStream = false;
                    try {
                        mLockCodec.wait();
                    } catch (InterruptedException e) {
                        // ignored
                    }
                }

                Log.i(TAG, "codec start");
                ByteBuffer[] outputBuffers = mEncoder.getOutputBuffers();
                while (true) {
                    synchronized (mLockCodec) {
                        if (mEndOfStream) {
                            if (VERBOSE) {
                                Log.d(TAG, "sending EOS to encoder");
                            }
                            mEncoder.signalEndOfInputStream();
                            mEndOfStream = false;
                        }
                    }
                    int outBufIndex = mEncoder.dequeueOutputBuffer(bufferInfo, 10_000);
                    if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        // no output available yet
                        if (VERBOSE) {
                            Log.d(TAG, "no output available, spinning to await EOS");
                        }
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        // not expected for an encoder
                        outputBuffers = mEncoder.getOutputBuffers();
                    } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // should happen before receiving buffers, and should only happen once
                        if (mMuxerStarted) {
                            throw new RuntimeException("format changed twice");
                        }
                        MediaFormat newFormat = mEncoder.getOutputFormat();
                        Log.d(TAG, "encoder output format changed: " + newFormat);

                        // now that we have the Magic Goodies, start the muxer
                        mTrackIndex = mMuxer.addTrack(newFormat);
                        mMuxer.start();
                        mMuxerStarted = true;
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
                                throw new RuntimeException("muxer hasn't started");
                            }

                            encodedData.position(bufferInfo.offset);
                            encodedData.limit(bufferInfo.offset + bufferInfo.size);
                            mMuxer.writeSampleData(mTrackIndex, encodedData, bufferInfo);
                            if (VERBOSE) {
                                Log.d(TAG, "sent " + bufferInfo.size + " bytes to muxer");
                            }
                        }
                        mEncoder.releaseOutputBuffer(outBufIndex, false);
                        if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.d(TAG, "end of stream reached");
                            break;      // out of while
                        }
                    }
                }

                Log.i(TAG, "release codec");
                if (mEncoder != null) {
                    mEncoder.stop();
                    mEncoder.release();
                    mEncoder = null;
                }
                if (mMuxer != null) {
                    mMuxer.stop();
                    mMuxer.release();
                    mMuxer = null;
                }
                if (mOnEncodeListener != null) {
                    mOnEncodeListener.onStop(outputPath);
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
                mEglCore = new EglCore(sharedContext, EglCore.FLAG_RECORDABLE);
                mWindowSurface = new WindowSurface(mEglCore, mInputSurface, true);
                mWindowSurface.makeCurrent();
                mTextureProgram = TextureProgram.createTextureOES();
                mInputSurface = null;

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
                        mWindowSurface.makeCurrent();
                        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                        mTextureProgram.drawFrame(mTexId, mTexMatrix, mMvpMatrix);
                        mWindowSurface.swapBuffers();
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
                if (mTextureProgram != null) {
                    mTextureProgram.release();
                    mTextureProgram = null;
                }
                if (mWindowSurface != null) {
                    mWindowSurface.release();
                    mWindowSurface = null;
                }
                if (mEglCore != null) {
                    mEglCore.release();
                    mEglCore = null;
                }
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
        }
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
