package com.richie.multimedialearning.player;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class PcmPlayer extends BaseMediaPlayer {
    private static final String TAG = "PcmPlayer";
    //编码 16 位深度
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    //采样频率 44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    private final static int AUDIO_SAMPLE_RATE = 16000;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    public static int INIT_ERROR = 0;
    public static int PLAY_ERROR = 1;
    public static int WRITE_ERROR = 2;
    public static int STOP_ERROR = 3;
    private volatile int mPlayState = STATE_IDLE;
    private Handler mPlayerHandler;
    // 总长度
    private AtomicInteger mDataLength = new AtomicInteger(0);
    // 播放的长度
    private AtomicInteger mPlayOffset = new AtomicInteger(0);
    // 字节流队列
    private ConcurrentLinkedQueue<byte[]> mDataQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean mIsPlaying;
    private AudioTrack mAudioTrack;
    // 声音参数
    private AudioParam mAudioParam;
    private float mAudioVolume = 1.0f;
    // 缓冲区字节大小
    private int mBufferSizeInBytes;

    public PcmPlayer() {
        AudioParam audioParam = new AudioParam();
        audioParam.mFrequency = AUDIO_SAMPLE_RATE;
        audioParam.mChannel = AUDIO_CHANNEL;
        audioParam.mSampleBit = AUDIO_ENCODING;
        mAudioParam = audioParam;
    }

    @Override
    public void prepareAsync() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
        }
        mBufferSizeInBytes = AudioTrack.getMinBufferSize(mAudioParam.mFrequency,
                mAudioParam.mChannel, mAudioParam.mSampleBit);
        Log.i(TAG, "prepareAsync: bufferSize:" + mBufferSizeInBytes);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mAudioParam.mFrequency,
                mAudioParam.mChannel, mAudioParam.mSampleBit, mBufferSizeInBytes, AudioTrack.MODE_STREAM);
        setVolume(mAudioVolume);

        if (AudioTrack.STATE_INITIALIZED != mAudioTrack.getState()) {
            Log.e(TAG, "AudioTrack state is not AudioTrack.STATE_INITIALIZED. the state is " + mAudioTrack.getState());
            mAudioTrack = null;
            notifyOnError(INIT_ERROR, "init error");
            return;
        }

        try {
            mAudioTrack.play();
            mPlayState = STATE_PREPARED;
            notifyOnPrepared();
        } catch (Exception e) {
            Log.e(TAG, "prepareAsync: ", e);
            notifyOnError(PLAY_ERROR, "play error");
        }

        if (mPlayerHandler == null) {
            HandlerThread handlerThread = new HandlerThread("pcm-player");
            handlerThread.start();
            mPlayerHandler = new Handler(handlerThread.getLooper());
        }
    }

    @Override
    public void setVolume(float audioVolume) {
        audioVolume = constrainAudioVolume(audioVolume);
        if (mAudioTrack != null) {
            mAudioTrack.setStereoVolume(audioVolume, audioVolume);
        }
        mAudioVolume = audioVolume;
    }

    @Override
    public int getAudioSessionId() {
        return mAudioTrack != null ? mAudioTrack.getAudioSessionId() : 0;
    }

    @Override
    public void setAudioSessionId(int sessionId) {
    }

    @Override
    public void setAudioStreamType(int type) {
    }

    public void write(byte[] buffer) {
        if (buffer == null) {
            buffer = new byte[0];
        }
        byte[] data = buffer;
        mDataQueue.add(data);
        mDataLength.set(mDataLength.get() + data.length);

        if (mPlayState == STATE_PREPARED || mPlayState == STATE_STARTED) {
            playBytes(data);
        }
    }

    private void playQueue() {
        for (byte[] data : mDataQueue) {
            playBytes(data);
        }
    }

    private void playBytes(final byte[] data) {
        if (mPlayerHandler == null) {
            return;
        }
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                int length = data.length;
                if (length == 0) {
                    mIsPlaying = false;
                    mPlayState = STATE_COMPLETED;
                    notifyOnCompletion();
                    return;
                }

                if (mAudioTrack != null) {
                    mPlayState = STATE_STARTED;
                    mIsPlaying = true;
                    try {
                        int segment;
                        if (length % mBufferSizeInBytes == 0) {
                            segment = length / mBufferSizeInBytes;
                        } else {
                            segment = length / mBufferSizeInBytes + 1;
                        }
                        for (int i = 0; i < segment; i++) {
                            mAudioTrack.write(data, i * mBufferSizeInBytes,
                                    i == segment - 1 ? length % mBufferSizeInBytes : mBufferSizeInBytes);
                        }
                        mDataQueue.remove(data);
                        mPlayOffset.set(mPlayOffset.get() + length);
                    } catch (Exception e) {
                        Log.e(TAG, "play: ", e);
                        mIsPlaying = false;
                        notifyOnError(WRITE_ERROR, "write error");
                    }
                }
            }
        });
    }

    @Override
    public void start() {
        playQueue();
    }

    @Override
    public void stop() {
        if (mPlayerHandler != null) {
            mPlayerHandler.removeCallbacksAndMessages(null);
        }
        reset();
        if (mAudioTrack != null) {
            try {
                mAudioTrack.stop();
                mAudioTrack.flush();
                mPlayState = STATE_STOPPED;
            } catch (Exception e) {
                Log.e(TAG, "stop: ", e);
                notifyOnError(STOP_ERROR, "stop error");
            }
        }
    }

    @Override
    public void pause() {
        stop();
    }

    @Override
    public void seekTo(long position) {
        if (mAudioTrack != null && position > 0L) {
            mPlayOffset.set((int) position);
        }
    }

    @Override
    public void reset() {
        mIsPlaying = false;
        mDataLength.set(0);
        mPlayOffset.set(0);
        mDataQueue.clear();
        mPlayState = STATE_IDLE;
    }

    @Override
    public void release() {
        reset();
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mPlayerHandler != null) {
            mPlayerHandler.getLooper().quitSafely();
            mPlayerHandler = null;
        }
        clearListener();
    }

    @Override
    public long getDuration() {
        if (mAudioParam.mChannel == AUDIO_CHANNEL) {
            return 1000L * mDataLength.get() / (mAudioParam.mFrequency * mAudioParam.mSampleBit);
        }
        return mDataLength.get();
    }

    @Override
    public long getCurrentPosition() {
        int position = 0;
        if (mAudioParam.mChannel == AUDIO_CHANNEL && mAudioTrack != null && mPlayState == STATE_STARTED) {
            try {
                position = (int) (1.0f * 1000 * mAudioTrack.getPlaybackHeadPosition() / mAudioTrack.getSampleRate());
            } catch (Exception e) {
                Log.e(TAG, "getCurrentPosition", e);
            }
        } else {
            position = mPlayOffset.get();
        }
        return position;
    }

    @Override
    public boolean isPlaying() {
        return mIsPlaying;
    }

    @Override
    public void setDataSource(String pathOrUrl) {
    }

    @Override
    public void setLooping(boolean isLooping) {
    }

    static class AudioParam {
        int mChannel;
        int mFrequency;
        int mSampleBit;
    }

}