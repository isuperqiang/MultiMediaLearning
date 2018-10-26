package com.richie.multimedialearning.audiorecord;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.richie.multimedialearning.utils.FileUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Richie on 2018.10.15
 */
public class AudioRecorder {
    private static final String TAG = "AudioRecorder";
    //音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率
    //44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    //采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    private final static int AUDIO_SAMPLE_RATE = 16000;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    //编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int mBufferSizeInBytes = 0;
    //录音对象
    private AudioRecord mAudioRecord;
    //录音状态
    private volatile Status mStatus = Status.STATUS_NO_READY;
    //文件名
    private String mFileName;
    //录音文件
    private List<String> mFilesName = new ArrayList<>();

    //线程池
    private ExecutorService mExecutorService = Executors.newCachedThreadPool();

    //录音监听
    private RecordStreamListener mRecordStreamListener;

    private Handler mMainHandler = new Handler(Looper.getMainLooper());

    private Context mContext;

    public AudioRecorder(Context context) {
        mContext = context;
    }

    /**
     * 创建录音对象
     */
    public void createAudio(String fileName, int audioSource, int sampleRateInHz, int channelConfig, int audioFormat) {
        // 获得缓冲区字节大小
        mBufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        mAudioRecord = new AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, mBufferSizeInBytes);
        int state = mAudioRecord.getState();
        Log.i(TAG, "createAudio state:" + state + ". initialized:" + (state == AudioRecord.STATE_INITIALIZED));
        mFileName = fileName;
        mStatus = Status.STATUS_READY;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    public void createDefaultAudio(String fileName) {
        createAudio(fileName, AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING);
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        if (mStatus == Status.STATUS_NO_READY || mAudioRecord == null) {
            throw new IllegalStateException("录音尚未初始化");
        }
        if (mStatus == Status.STATUS_START) {
            throw new IllegalStateException("正在录音...");
        }
        Log.d(TAG, "===startRecord===");
        mAudioRecord.startRecording();

        String currentFileName = FileUtils.getPcmFilePath(mContext, mFileName);
        if (mStatus == Status.STATUS_PAUSE) {
            //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
            currentFileName += mFilesName.size();
        }
        mFilesName.add(currentFileName);

        final String finalFileName = currentFileName;
        //将录音状态设置成正在录音状态
        mStatus = Status.STATUS_START;

        //使用线程池管理线程
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                writeDataToFile(finalFileName);
            }
        });
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        Log.d(TAG, "===pauseRecord===");
        if (mStatus != Status.STATUS_START) {
            throw new IllegalStateException("没有在录音");
        } else {
            mAudioRecord.stop();
            mStatus = Status.STATUS_PAUSE;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        Log.d(TAG, "===stopRecord===");
        if (mStatus == Status.STATUS_NO_READY || mStatus == Status.STATUS_READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            mAudioRecord.stop();
            mStatus = Status.STATUS_STOP;
            release();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "===release===");
        //假如有暂停录音
        try {
            if (mFilesName.size() > 0) {
                List<String> filePaths = new ArrayList<>(mFilesName);
                //清除
                mFilesName.clear();
                //将多个pcm文件转化为wav文件
                mergePCMFilesToWAVFile(filePaths);
            } else {
                //这里由于只要录音过filesName.size都会大于0,没录音时fileName为null
                //会报空指针 NullPointerException
                // 将单个pcm文件转化为wav文件
                Log.d(TAG, "=====makePCMFileToWAVFile======");
                //makePCMFileToWAVFile();
            }
        } catch (IllegalStateException e) {
            throw new IllegalStateException(e.getMessage());
        }

        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mStatus = Status.STATUS_NO_READY;
    }

    /**
     * 取消录音
     */
    public void canel() {
        mFilesName.clear();
        mFileName = null;
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mStatus = Status.STATUS_NO_READY;
    }


    /**
     * 将音频信息写入文件
     *
     * @param currentFileName
     */
    private void writeDataToFile(String currentFileName) {
        byte[] audioData = new byte[mBufferSizeInBytes];
        BufferedOutputStream bos = null;
        int readSize;
        try {
            File file = new File(currentFileName);
            if (file.exists()) {
                file.delete();
            }
            bos = new BufferedOutputStream(new FileOutputStream(file));

            while (mStatus == Status.STATUS_START) {
                readSize = mAudioRecord.read(audioData, 0, mBufferSizeInBytes);
                if (AudioRecord.ERROR_INVALID_OPERATION != readSize) {
                    try {
                        bos.write(audioData);
                        if (mRecordStreamListener != null) {
                            mRecordStreamListener.onRecording(audioData, 0, audioData.length);
                        }
                    } catch (IOException e) {
                        Log.e(TAG, e.getMessage());
                    }
                }
            }
            bos.flush();
            if (mRecordStreamListener != null) {
                mRecordStreamListener.finishRecord();
            }
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        } finally {
            try {
                if (bos != null) {
                    bos.close();// 关闭写入流
                }
            } catch (IOException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }

    /**
     * 将pcm合并成wav
     *
     * @param filePaths
     */
    private void mergePCMFilesToWAVFile(final List<String> filePaths) {
        Log.d(TAG, "mergePCMFilesToWAVFile: " + filePaths);
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                String wavFilePath = FileUtils.getWavFilePath(mContext, mFileName);
                if (PcmToWav.mergePCMFilesToWAVFile(filePaths, wavFilePath)) {
                    //操作成功
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "保存wav文件列表成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    //操作失败
                    Log.e(TAG, "mergePCMFilesToWAVFile fail");
                    throw new IllegalStateException("mergePCMFilesToWAVFile fail");
                }
            }
        });
    }

    /**
     * 将单个pcm文件转化为wav文件
     */
    private void makePCMFileToWAVFile() {
        mExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                if (PcmToWav.makePCMFileToWAVFile("", FileUtils.getWavFilePath(mContext, mFileName), false)) {
                    //操作成功
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(mContext, "保存wav文件成功", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    //操作失败
                    Log.e(TAG, "makePCMFileToWAVFile fail");
                    throw new IllegalStateException("makePCMFileToWAVFile fail");
                }
            }
        });
    }

    /**
     * 获取录音对象的状态
     *
     * @return
     */
    public Status getStatus() {
        return mStatus;
    }

    /**
     * 获取本次录音文件的个数
     *
     * @return
     */
    public int getPcmFilesCount() {
        return mFilesName.size();
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
        //暂停
        STATUS_PAUSE,
        //停止
        STATUS_STOP
    }

    public interface RecordStreamListener {
        void onRecording(byte[] bytes, int offset, int length);

        void finishRecord();
    }
}

