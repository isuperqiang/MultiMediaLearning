package com.richie.multimedialearning.player;

import android.content.Context;

/**
 * @author LiuQiang on 2019.01.28
 */
public abstract class BaseMediaPlayer {
    /**
     * An extensible media player for Android
     */
    public static final int TYPE_EXO = 1;
    /**
     * play pcm stream by AudioTrack
     */
    public static final int TYPE_PCM = 2;
    /**
     * Android SDK MediaPlayer, not recommended
     */
    public static final int TYPE_PLATFORM = 3;

    /* state, see Android MediaPlayer's state */
    public static final int STATE_IDLE = -1;
    public static final int STATE_INITIALIZED = 0;
    public static final int STATE_PREPARING = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_STARTED = 3;
    public static final int STATE_PAUSED = 4;
    public static final int STATE_STOPPED = 5;
    public static final int STATE_COMPLETED = 6;
    public static final int STATE_RELEASED = 7;

    protected OnErrorListener mOnErrorListener;
    protected OnCompletionListener mOnCompletionListener;
    protected OnPreparedListener mOnPreparedListener;

    public static BaseMediaPlayer createPlayer(int type, Context context) {
        switch (type) {
            case BaseMediaPlayer.TYPE_EXO:
                return new ExoMediaPlayer(context);
            case BaseMediaPlayer.TYPE_PCM:
                return new PcmPlayer();
            case BaseMediaPlayer.TYPE_PLATFORM:
                return new AndroidMediaPlayer();
            default:
                return new ExoMediaPlayer(context);
        }
    }

    public abstract void start();

    public abstract void pause();

    public abstract void stop();

    public abstract void release();

    public abstract void seekTo(long position);

    public abstract long getDuration();

    public abstract long getCurrentPosition();

    public abstract void setDataSource(String pathOrUrl);

    public abstract boolean isPlaying();

    public abstract void setLooping(boolean isLooping);

    public abstract void reset();

    public abstract void prepareAsync();

    public abstract void setVolume(float audioVolume);

    public abstract int getAudioSessionId();

    public abstract void setAudioSessionId(int sessionId);

    public abstract void setAudioStreamType(int type);

    protected void notifyOnPrepared() {
        if (mOnPreparedListener != null) {
            mOnPreparedListener.onPrepared(this);
        }
    }

    protected void notifyOnCompletion() {
        if (mOnCompletionListener != null) {
            mOnCompletionListener.onCompletion(this);
        }
    }

    protected void notifyOnError(int type, String message) {
        if (mOnErrorListener != null) {
            mOnErrorListener.onError(this, type, message);
        }
    }

    protected void clearListener() {
        mOnPreparedListener = null;
        mOnCompletionListener = null;
        mOnErrorListener = null;
    }

    protected float constrainAudioVolume(float value) {
        return Math.max(0, Math.min(value, 1));
    }

    public void setOnPreparedListener(OnPreparedListener listener) {
        mOnPreparedListener = listener;
    }

    public void setOnCompletionListener(OnCompletionListener listener) {
        mOnCompletionListener = listener;
    }

    public void setOnErrorListener(OnErrorListener listener) {
        mOnErrorListener = listener;
    }

    public interface OnPreparedListener {
        /**
         * Called when the media file is ready for playback.
         *
         * @param player
         */
        void onPrepared(BaseMediaPlayer player);
    }

    public interface OnCompletionListener {
        /**
         * Called when the end of a media source is reached during playback.
         *
         * @param player
         */
        void onCompletion(BaseMediaPlayer player);
    }

    public interface OnErrorListener {
        /**
         * Called to indicate an error.
         *
         * @param player
         * @param type
         * @param message
         */
        void onError(BaseMediaPlayer player, int type, String message);
    }
}
