package com.richie.multimedialearning.media;

import android.media.MediaFormat;

import java.nio.ByteBuffer;

/**
 * @author Richie on 2020.12.29
 */
public interface IExtractor {
    /**
     * 获取格式
     *
     * @return
     */
    MediaFormat getFormat();

    /**
     * 读取数据
     *
     * @param buffer
     * @return
     */
    int readBuffer(ByteBuffer buffer);

    /**
     * 获取当前帧时间
     *
     * @return
     */
    long getSampleTimeStamp();

    /**
     * 获取当前帧 flag
     *
     * @return
     */
    int getSampleFlags();

    /**
     * 跳转到指定位置，返回实际帧的时间戳
     *
     * @param position
     * @return
     */
    long seek(long position);

    /**
     * 设置起始位置
     *
     * @param position
     */
    void setStartPosition(long position);

    /**
     * 停止读取数据
     */
    void stop();
}
