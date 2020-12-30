package com.richie.multimedialearning.media;

/**
 * @author Richie on 2020.12.30
 */
public interface IDecoderProgressListener {
    /**
     * 视频宽高变化
     *
     * @param width
     * @param height
     * @param rotation
     */
    void videoSizeChanged(int width, int height, int rotation);

    /**
     * 视频播放进度
     *
     * @param progress
     */
    void videoProgressChanged(long progress);
}
