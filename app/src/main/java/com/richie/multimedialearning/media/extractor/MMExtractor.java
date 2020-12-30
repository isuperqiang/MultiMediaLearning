package com.richie.multimedialearning.media.extractor;

import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音视频分离器
 *
 * @author Richie on 2020.12.30
 */
class MMExtractor {
    private static final String TAG = "MMExtractor";

    private MediaExtractor mMediaExtractor;
    private int mAudioTrack = -1;
    private int mVideoTrack = -1;
    private long mSampleTime;
    private int mSampleFlags;
    private long mStartPosition;

    MMExtractor(String filePath) {
        mMediaExtractor = new MediaExtractor();
        try {
            mMediaExtractor.setDataSource(filePath);
        } catch (IOException e) {
            Log.e(TAG, "MMExtractor: ", e);
        }
    }

    /**
     * 获取视频格式参数
     *
     * @return
     */
    MediaFormat getVideoFormat() {
        int trackCount = mMediaExtractor.getTrackCount();
        MediaFormat format = null;
        for (int i = 0; i < trackCount; i++) {
            format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                mVideoTrack = i;
                break;
            }
        }
        return format;
    }

    /**
     * 获取音频格式参数
     *
     * @return
     */
    MediaFormat getAudioFormat() {
        int trackCount = mMediaExtractor.getTrackCount();
        MediaFormat format = null;
        for (int i = 0; i < trackCount; i++) {
            format = mMediaExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("audio/")) {
                mAudioTrack = i;
                break;
            }
        }
        return format;
    }

    /**
     * 读取数据
     *
     * @param buffer
     * @return
     */
    int readBuffer(ByteBuffer buffer) {
        buffer.clear();
        selectSourceTrack();
        int sampleSize = mMediaExtractor.readSampleData(buffer, 0);
        if (sampleSize < 0) {
            return -1;
        }
        mSampleTime = mMediaExtractor.getSampleTime();
        mSampleFlags = mMediaExtractor.getSampleFlags();
        mMediaExtractor.advance();
        return sampleSize;
    }

    /**
     * 跳转指定位置，并返回帧时间戳
     *
     * @param pos
     * @return
     */
    long seek(long pos) {
        mMediaExtractor.seekTo(pos, MediaExtractor.SEEK_TO_PREVIOUS_SYNC);
        return mMediaExtractor.getSampleTime();
    }

    /**
     * 停止读取数据
     */
    void stop() {
        mMediaExtractor.release();
        mMediaExtractor = null;
    }

    int getVideoTrack() {
        return mVideoTrack;
    }

    int getAudioTrack() {
        return mAudioTrack;
    }

    void setStartPosition(long position) {
        mStartPosition = position;
    }

    long getSampleTime() {
        return mSampleTime;
    }

    int getSampleFlags() {
        return mSampleFlags;
    }

    private void selectSourceTrack() {
        if (mVideoTrack >= 0) {
            mMediaExtractor.selectTrack(mVideoTrack);
        } else if (mAudioTrack >= 0) {
            mMediaExtractor.selectTrack(mAudioTrack);
        }
    }

}
