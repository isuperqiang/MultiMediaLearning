package com.richie.multimedialearning.audiotrack;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Richie on 2018.10.26
 */
public class AudioTracker {
    private static final String TAG = "AudioTracker";
    //采用频率
    //44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    //采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 16000;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    //编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int mBufferSizeInBytes = 0;
    //播放对象
    private AudioTrack mAudioTrack;
    //文件名
    private String mFilePath;
    // 状态
    private volatile Status mStatus = Status.STATUS_NO_READY;
    //线程池
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private Context mContext;

    public AudioTracker(Context context) {
        mContext = context;
    }

    public void createAudioTrack(String filePath) {
        mFilePath = filePath;
        mBufferSizeInBytes = AudioTrack.getMinBufferSize(AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING,
                mBufferSizeInBytes, AudioTrack.MODE_STREAM);
        mStatus = Status.STATUS_READY;
    }

    /**
     * 开始播放
     */
    public void start() {
        if (mStatus == Status.STATUS_NO_READY || mAudioTrack == null) {
            throw new IllegalStateException("播放器尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在播放...");
        }
        Log.d(TAG, "===start===");
        if (mStatus == Status.STATUS_PAUSE) {
            mAudioTrack.play();
        } else {
            mExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        writeAudioData();
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                        mMainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "播放出错", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
        mStatus = Status.STATUS_START;
    }

    private void writeAudioData() throws IOException {
        DataInputStream dis = null;
        try {
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "播放开始", Toast.LENGTH_SHORT).show();
                }
            });
            FileInputStream fis = new FileInputStream(mFilePath);
            dis = new DataInputStream(new BufferedInputStream(fis));
            byte[] bytes = new byte[mBufferSizeInBytes];
            int len;
            mAudioTrack.play();
            while ((len = dis.read(bytes)) != -1 && mStatus == Status.STATUS_START) {
                mAudioTrack.write(bytes, 0, len);
            }
            dis.close();
            mMainHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, "播放结束", Toast.LENGTH_SHORT).show();
                }
            });
        } finally {
            if (dis != null) {
                dis.close();
            }
        }
    }

    /**
     * 暂停播放
     */
    @Deprecated
    public void pause() {
        Log.d(TAG, "===pause===");
        if (mStatus != Status.STATUS_START) {
            throw new IllegalStateException("没有在播放");
        } else {
            mAudioTrack.pause();
            mStatus = Status.STATUS_PAUSE;
        }
    }

    public void stop() {
        Log.d(TAG, "===stop===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("播放尚未开始");
        } else {
            mAudioTrack.stop();
            mStatus = Status.STATUS_STOP;
            release();
        }
    }

    public void release() {
        Log.d(TAG, "==release===");
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
            mStatus = Status.STATUS_NO_READY;
        }
    }

    /**
     * 播放对象的状态
     */
    public enum Status {
        //未开始
        STATUS_NO_READY,
        //预备
        STATUS_READY,
        //播放
        STATUS_START,
        //暂停
        STATUS_PAUSE,
        //停止
        STATUS_STOP
    }
}
