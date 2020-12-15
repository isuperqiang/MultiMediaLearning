package com.richie.multimedialearning.player;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 播放器控制类，采用单线程模型
 *
 * @author Richie on 2019.03.11
 */
public class MediaPlayerHandler {
    private Handler mPlayerHandler;
    private long mCurrentPosition;
    private long mDuration;
    private Lock mLock = new ReentrantLock();
    private Condition mGetCurrentPositionCondition = mLock.newCondition();
    private Condition mGetDurationCondition = mLock.newCondition();
    private Context mContext;
    private volatile BaseMediaPlayer mBasePlayer;
    private Runnable mGetCurrentPositionRunnable = new Runnable() {
        @Override
        public void run() {
            mLock.lock();
            try {
                if (mBasePlayer != null) {
                    mCurrentPosition = mBasePlayer.getCurrentPosition();
                }
                mGetCurrentPositionCondition.signal();
            } finally {
                mLock.unlock();
            }
        }
    };

    public MediaPlayerHandler(Context context) {
        mContext = context;
        HandlerThread playerThread = new HandlerThread("media-player", Process.THREAD_PRIORITY_BACKGROUND);
        playerThread.start();
        mPlayerHandler = new Handler(playerThread.getLooper());
    }

    public void setDataSource(String pathOrUrl) {
        if (mBasePlayer != null) {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBasePlayer.setDataSource(pathOrUrl);
                }
            });
        }
    }

    public void startMediaPlayer() {
        if (mBasePlayer != null) {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBasePlayer.start();
                }
            });
        }
    }

    public void stopMediaPlayer() {
        if (mBasePlayer != null) {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBasePlayer.stop();
                }
            });
        }
    }

    public void pauseMediaPlayer() {
        if (mBasePlayer != null) {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBasePlayer.pause();
                }
            });
        }
    }

    public long getCurrentPosition() {
        mLock.lock();
        try {
            mPlayerHandler.post(mGetCurrentPositionRunnable);
            mGetCurrentPositionCondition.await();
        } catch (InterruptedException e) {
            // ignored
        } finally {
            mLock.unlock();
        }
        return mCurrentPosition;
    }

    public void releaseMediaPlayer() {
        if (mBasePlayer != null) {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mBasePlayer.release();
                    mBasePlayer = null;
                }
            });
        }

        mPlayerHandler.getLooper().quitSafely();
        mPlayerHandler = null;
    }

    public long getDuration() {
        mLock.lock();
        try {
            mPlayerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mLock.lock();
                    try {
                        if (mBasePlayer != null) {
                            mDuration = mBasePlayer.getDuration();
                        }
                        mGetDurationCondition.signal();
                    } finally {
                        mLock.unlock();
                    }
                }
            });
            mGetDurationCondition.await();
        } catch (InterruptedException e) {
            // ignored
        } finally {
            mLock.unlock();
        }
        return mDuration;
    }

    /**
     * Initialize media player in handlerThread
     * All player method is invoked in handlerThread. Single thread model is easy to control.
     *
     * @param playerType
     * @param onPreparedListener
     * @param onCompletionListener
     * @param onErrorListener
     */
    public void initPlayer(int playerType, BaseMediaPlayer.OnPreparedListener onPreparedListener,
                           BaseMediaPlayer.OnCompletionListener onCompletionListener, BaseMediaPlayer.OnErrorListener onErrorListener) {
        mPlayerHandler.post(new Runnable() {
            @Override
            public void run() {
                mBasePlayer = BaseMediaPlayer.createPlayer(playerType, mContext);
                mBasePlayer.setOnPreparedListener(onPreparedListener);
                mBasePlayer.setOnCompletionListener(onCompletionListener);
                mBasePlayer.setOnErrorListener(onErrorListener);
            }
        });
    }

}
