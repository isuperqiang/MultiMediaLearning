package com.richie.multimedialearning.audiorecord;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.wav.PcmToWav;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Richie on 2018.10.15
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    // 输入源 麦克风
    private final static int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    // 采样率 44100Hz，所有设备都支持
    private final static int SAMPLE_RATE = 44100;
    // 通道 单声道，所有设备都支持
    private final static int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    // 位深 16 位，所有设备都支持
    private final static int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int mBufferSizeInBytes;
    // 单任务线程池
    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
    // 录音对象
    private AudioRecord mAudioRecord;
    // 录音状态
    private volatile Status mStatus = Status.STATUS_NO_READY;
    // 文件名
    private String mPcmFileName;
    // 录音监听
    private RecordStreamListener mRecordStreamListener;

    private Context mContext;

    public AudioRecorder(Context context) {
        mContext = context;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    public void createDefaultAudio(String fileName) {
        createAudio(fileName, AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
    }

    /**
     * 创建录音对象
     *
     * @param fileName
     * @param audioSource
     * @param sampleRateInHz
     * @param channelConfig
     * @param audioFormat
     */
    public void createAudio(String fileName, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) throws IllegalStateException {
        // 获得缓冲区字节大小
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (mBufferSizeInBytes <= 0) {
            throw new IllegalStateException("AudioRecord is not available " + mBufferSizeInBytes);
        }

        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state:" + state + ", initialized:" + (state == AudioRecord.STATE_INITIALIZED));
        mPcmFileName = fileName;
        mStatus = Status.STATUS_READY;
    }

    /**
     * 开始录音
     */
    public void startRecord() throws IllegalStateException {
        if (mStatus == Status.STATUS_NO_READY || mAudioRecord == null) {
            throw new IllegalStateException("录音尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在录音...");
        }
        Log.d(TAG, "===startRecord===");
        mAudioRecord.startRecording();

        //将录音状态设置成正在录音状态
        mStatus = Status.STATUS_START;

        //使用线程池管理线程
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    writeAudioDataToFile();
                } catch (IOException e) {
                    Log.e(TAG, "writeDataToFile: ", e);
                }
            }
        });
    }

    /**
     * 停止录音
     */
    public void stopRecord() throws IllegalStateException {
        Log.d(TAG, "===stopRecord===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            mStatus = Status.STATUS_STOP;
            mAudioRecord.stop();
            release();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "===release===");
        mStatus = Status.STATUS_NO_READY;

        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        makePCMFileToWAVFile();
    }

    /**
     * 取消录音
     */
    public void canel() {
        mPcmFileName = null;
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mStatus = Status.STATUS_NO_READY;
    }


    /**
     * 将音频信息写入文件
     */
    private void writeAudioDataToFile() throws IOException {
        String pcmFilePath = FileUtils.getPcmFilePath(mContext, mPcmFileName);
        File file = new File(pcmFilePath);
        if (file.exists()) {
            file.delete();
        }
        OutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(file));
            byte[] audioData = new byte[mBufferSizeInBytes];
            while (mStatus == Status.STATUS_START) {
                int readSize = mAudioRecord.read(audioData, 0, mBufferSizeInBytes);
                if (readSize > 0) {
                    try {
                        bos.write(audioData, 0, readSize);
                        if (mRecordStreamListener != null) {
                            mRecordStreamListener.onRecording(audioData, 0, readSize);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "writeAudioDataToFile", e);
                    }
                } else {
                    Log.w(TAG, "writeAudioDataToFile readSize: " + readSize);
                }
            }
            bos.flush();
            if (mRecordStreamListener != null) {
                mRecordStreamListener.finishRecord();
            }
        } finally {
            if (bos != null) {
                bos.close();// 关闭写入流
            }
        }
    }

    /**
     * 将单个pcm文件转化为wav文件
     */
    private void makePCMFileToWAVFile() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                String wavFilePath = FileUtils.getWavFilePath(mContext, mPcmFileName);
                if (PcmToWav.makePCMFileToWAVFile(FileUtils.getPcmFilePath(mContext, mPcmFileName), wavFilePath, false)) {
                    //操作成功
                    Log.i(TAG, "保存wav文件成功 " + wavFilePath);
                } else {
                    //操作失败
                    Log.e(TAG, "makePCMFileToWAVFile fail");
                }
            }
        });
    }

    public void setRecordStreamListener(RecordStreamListener recordStreamListener) {
        this.mRecordStreamListener = recordStreamListener;
    }

    /**
     * 录音对象的状态
     */
    public enum Status {
        //未开始
        STATUS_NO_READY,
        //预备
        STATUS_READY,
        //录音
        STATUS_START,
        //停止
        STATUS_STOP
    }

    /**
     * invoked from work thread
     */
    public interface RecordStreamListener {
        /**
         * 录音过程中
         *
         * @param bytes
         * @param offset
         * @param length
         */
        void onRecording(byte[] bytes, int offset, int length);

        /**
         * 录音完成
         */
        void finishRecord();
    }
}

