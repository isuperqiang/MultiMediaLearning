package cn.richie.ffmpeg;

import android.view.Surface;

/**
 * @author Richie on 2020.12.15
 */
public class FFmpegNative {
    static {
        System.loadLibrary("ffmpegnative");
    }

    private long mPlayerHandle;

    public static native String getVersion();

    public native void createPlayer(String path, Surface surface);

    public native void play();

    public native void pause();

    public native void releasePlayer();
}
