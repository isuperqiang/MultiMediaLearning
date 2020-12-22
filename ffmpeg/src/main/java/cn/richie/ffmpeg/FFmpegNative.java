package cn.richie.ffmpeg;

/**
 * @author Richie on 2020.12.15
 */
public class FFmpegNative {
    static {
        System.loadLibrary("ffmpegnative");
    }

    public static native String getVersion();
}
