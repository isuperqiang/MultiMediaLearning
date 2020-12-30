package com.richie.multimedialearning.media;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;

/**
 * 基础解码器
 *
 * @author Richie on 2020.12.29
 */
public abstract class BaseDecoder implements IDecoder {
    private static final String TAG = "BaseDecoder";
    public static final int TIMEOUT_US = 1000;

    private final Object mLock = new Object();
    private volatile boolean mIsRunning = true;
    private boolean mReadyForDecode = false;

    private MediaCodec mMediaCodec;
    private IExtractor mExtractor;
    private ByteBuffer[] mInputBuffers;
    private ByteBuffer[] mOutputBuffers;
    private final MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
    private DecodeState mState = DecodeState.STOP;
    protected IDecodeStateListener mStateListener;
    private boolean mIsEos = false;
    protected int mVideoWidth;
    protected int mVideoHeight;
    private long mDuration;
    private long mStartPos;
    private long mEndPos;
    private long mStartTimeForSync = -1;
    private boolean mSyncRender = true;
    private final String mFilePath;

    protected BaseDecoder(String filePath) {
        mFilePath = filePath;
    }

    @Override
    public void run() {
        if (mState == DecodeState.STOP) {
            mState = DecodeState.START;
        }
        if (mStateListener != null) {
            mStateListener.decoderPrepare(this);
        }
        if (!init()) {
            return;
        }
        Log.i(TAG, "run: start decode " + getClass().getSimpleName() + ", state " + mState);
        try {
            while (mIsRunning) {
                if (mState != DecodeState.START && mState != DecodeState.DECODING && mState != DecodeState.SEEKING) {
                    Log.i(TAG, "run: wait decode " + getClass().getSimpleName() + ", state " + mState);
                    waitDecode();
                    // 同步时间
                    mStartTimeForSync = System.currentTimeMillis() - getCurrTimeStamp();
                }

                if (!mIsRunning || mState == DecodeState.STOP) {
                    mIsRunning = false;
                    break;
                }

                if (mStartTimeForSync == -1) {
                    mStartTimeForSync = System.currentTimeMillis();
                }

                if (!mIsEos) {
                    mIsEos = pushBufferToDecoder();
                }

                int index = pullBufferFromDecoder();
                if (index >= 0) {
                    if (mSyncRender && mState == DecodeState.DECODING) {
                        sleepRender();
                    }
                    if (mSyncRender) {
                        render(mOutputBuffers[index], mBufferInfo);
                    }
//                    if (mDecodeStateListener != null) {
//                        Frame frame = new Frame();
//                        frame.mByteBuffer = mOutputBuffers[index];
//                        frame.setBufferInfo(mBufferInfo);
//                        mDecodeStateListener.decoderOneFrame(this, frame);
//                    }
                    mMediaCodec.releaseOutputBuffer(index, true);
                    if (mState == DecodeState.START) {
                        mState = DecodeState.PAUSE;
                    }
                }
                if (mBufferInfo.flags == MediaCodec.BUFFER_FLAG_END_OF_STREAM) {
                    Log.i(TAG, "run: decode finish " + getClass().getSimpleName());
                    mState = DecodeState.FINISH;
                    if (mStateListener != null) {
                        mStateListener.decoderFinish(this);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "run: ", e);
        } finally {
            doneDecode();
            release();
        }
    }

    private boolean init() {
        Log.d(TAG, "init: ");
        if (mFilePath.isEmpty() || !new File(mFilePath).exists()) {
            return false;
        }
        if (!check()) {
            return false;
        }
        mExtractor = initExtractor(mFilePath);
        if (mExtractor == null || mExtractor.getFormat() == null) {
            Log.w(TAG, "init: 无法解析文件");
            return false;
        }
        if (!initParams()) {
            return false;
        }
        if (!initRender()) {
            return false;
        }
        if (!initCodec()) {
            return false;
        }
        return true;
    }

    private boolean initCodec() {
        Log.d(TAG, "initCodec: " + getClass().getSimpleName());
        try {
            MediaFormat format = mExtractor.getFormat();
            String mime = format.getString(MediaFormat.KEY_MIME);
            mMediaCodec = MediaCodec.createDecoderByType(mime);
            if (!configCodec(mMediaCodec, format)) {
                waitDecode();
            }
            mMediaCodec.start();
            mInputBuffers = mMediaCodec.getInputBuffers();
            mOutputBuffers = mMediaCodec.getOutputBuffers();
            return true;
        } catch (Exception e) {
            Log.w(TAG, "initCodec: ", e);
            return false;
        }
    }

    private boolean initParams() {
        Log.d(TAG, "initParams: ");
        try {
            MediaFormat format = mExtractor.getFormat();
            mDuration = format.getLong(MediaFormat.KEY_DURATION) / 1000;
            if (mEndPos == 0) {
                mEndPos = mDuration;
            }
            initSpecParams(format);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "initParams: ", e);
            return false;
        }
    }

    private boolean pushBufferToDecoder() {
        int index = mMediaCodec.dequeueInputBuffer(TIMEOUT_US);
        boolean isEos = false;
        if (index >= 0) {
            ByteBuffer inputBuffer = mInputBuffers[index];
            int sampleSize = mExtractor.readBuffer(inputBuffer);
            if (sampleSize < 0) {
                mMediaCodec.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                isEos = true;
            } else {
                mMediaCodec.queueInputBuffer(index, 0, sampleSize, mExtractor.getSampleTimeStamp(), 0);
            }
        }
        return isEos;
    }

    private int pullBufferFromDecoder() {
        int index = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_US);
        switch (index) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED: {
                mOutputBuffers = mMediaCodec.getOutputBuffers();
            }
            break;
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_TRY_AGAIN_LATER:
            default:
                break;
        }
        return index;
    }

    private void sleepRender() {
        long passTime = System.currentTimeMillis() - mStartTimeForSync;
        long currTime = getCurrTimeStamp();
        if (currTime > passTime) {
            SystemClock.sleep(currTime - passTime);
        }
    }

    private void release() {
        Log.d(TAG, "release: " + getClass().getSimpleName());
        mState = DecodeState.STOP;
        mIsEos = false;
        try {
            mExtractor.stop();
            mMediaCodec.stop();
            mMediaCodec.release();
        } catch (Exception e) {
            Log.w(TAG, "release: ", e);
        }
        if (mStateListener != null) {
            mStateListener.decoderDestroy(this);
        }
    }

    private void waitDecode() {
        if (mState == DecodeState.PAUSE) {
            if (mStateListener != null) {
                mStateListener.decoderPause(this);
            }
        }
        synchronized (mLock) {
            try {
                Log.w(TAG, "waitDecode: wait" + getClass().getSimpleName());
                mLock.wait();
            } catch (InterruptedException e) {
                Log.w(TAG, "waitDecode: ", e);
            }
        }
    }

    protected void notifyDecode() {
        synchronized (mLock) {
            mLock.notifyAll();
        }
        if (mState == DecodeState.DECODING) {
            if (mStateListener != null) {
                mStateListener.decoderRunning(this);
            }
        }
    }

    @Override
    public void pause() {
        mState = DecodeState.PAUSE;
    }

    @Override
    public void goOn() {
        mState = DecodeState.DECODING;
        notifyDecode();
    }

    @Override
    public long seekTo(long pos) {
        return 0;
    }

    @Override
    public long seekAndPlay(long pos) {
        return 0;
    }

    @Override
    public void stop() {
        mState = DecodeState.STOP;
        mIsRunning = false;
        notifyDecode();
    }

    @Override
    public boolean isDecoding() {
        return mState == DecodeState.DECODING;
    }

    @Override
    public boolean isSeeking() {
        return mState == DecodeState.SEEKING;
    }

    @Override
    public boolean isStopped() {
        return mState == DecodeState.STOP;
    }

    @Override
    public void setSizeListener(IDecoderProgressListener decoderProgressListener) {

    }

    @Override
    public void setStateListener(IDecodeStateListener decodeStateListener) {
        mStateListener = decodeStateListener;
    }

    @Override
    public int getWidth() {
        return mVideoWidth;
    }

    @Override
    public int getHeight() {
        return mVideoHeight;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public long getCurrTimeStamp() {
        return mBufferInfo.presentationTimeUs / 1000;
    }

    @Override
    public int getRotation() {
        return 0;
    }

    @Override
    public MediaFormat getMediaFormat() {
        return mExtractor.getFormat();
    }

    @Override
    public int getTrackCount() {
        return 0;
    }

    @Override
    public String getFilePath() {
        return mFilePath;
    }

    @Override
    public IDecoder withoutSync() {
        mSyncRender = false;
        return this;
    }

    /**
     * 检测子类参数
     *
     * @return
     */
    protected abstract boolean check();

    /**
     * 初始化提取器
     *
     * @param filePath
     * @return
     */
    protected abstract IExtractor initExtractor(String filePath);

    /**
     * 初始化子类特有的参数
     *
     * @param mediaFormat
     */
    protected abstract void initSpecParams(MediaFormat mediaFormat);

    /**
     * 配置解码器
     *
     * @param mediaCodec
     * @param mediaFormat
     * @return
     */
    protected abstract boolean configCodec(MediaCodec mediaCodec, MediaFormat mediaFormat);

    /**
     * 初始化渲染器
     *
     * @return
     */
    protected abstract boolean initRender();

    /**
     * 渲染
     *
     * @param outputBuffer
     * @param bufferInfo
     */
    protected abstract void render(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo);

    /**
     * 解码结束
     */
    protected abstract void doneDecode();

}
