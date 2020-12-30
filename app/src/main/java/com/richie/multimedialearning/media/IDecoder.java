package com.richie.multimedialearning.media;

import android.media.MediaFormat;

/**
 * 解码器接口
 *
 * @author Richie on 2020.12.29
 */
public interface IDecoder extends Runnable {
    /**
     * 暂停解码
     */
    void pause();

    /**
     * 继续解码
     */
    void goOn();

    /**
     * 跳转到指定位置，返回实际帧的时间
     *
     * @param pos
     * @return
     */
    long seekTo(long pos);

    /**
     * 跳转到指定位置并播放，返回实际帧的时间
     *
     * @param pos
     * @return
     */
    long seekAndPlay(long pos);

    /**
     * 停止解码
     */
    void stop();

    /**
     * 是否正在解码
     *
     * @return
     */
    boolean isDecoding();

    /**
     * 是否正在跳转
     *
     * @return
     */
    boolean isSeeking();

    /**
     * 是否停止解码
     *
     * @return
     */
    boolean isStopped();

    void setSizeListener(IDecoderProgressListener decoderProgressListener);

    void setStateListener(IDecodeStateListener decodeStateListener);

    /**
     * 获取宽度
     *
     * @return
     */
    int getWidth();

    /**
     * 获取高度
     *
     * @return
     */
    int getHeight();

    /**
     * 获取时长
     *
     * @return
     */
    long getDuration();

    /**
     * 获取当前帧时间
     *
     * @return
     */
    long getCurrTimeStamp();

    /**
     * 获取旋转角度
     *
     * @return
     */
    int getRotation();

    /**
     * 获取音视频格式
     *
     * @return
     */
    MediaFormat getMediaFormat();

    /**
     * 获取音视频轨道数
     *
     * @return
     */
    int getTrackCount();

    /**
     * 获取解码的文件路径
     *
     * @return
     */
    String getFilePath();

    /**
     * 无需音视频同步
     *
     * @return
     */
    IDecoder withoutSync();
}
