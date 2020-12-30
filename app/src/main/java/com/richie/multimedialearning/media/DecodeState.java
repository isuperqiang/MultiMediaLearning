package com.richie.multimedialearning.media;

/**
 * @author Richie on 2020.12.29
 */
public enum DecodeState {
    /**
     * 开始状态
     */
    START,
    /**
     * 解码中
     */
    DECODING,
    /**
     * 解码暂停
     */
    PAUSE,
    /**
     * 正在快进
     */
    SEEKING,
    /**
     * 解码完成
     */
    FINISH,
    /**
     * 解码器释放
     */
    STOP
}
