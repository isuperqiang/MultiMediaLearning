package com.richie.multimedialearning.ffmpeg;

import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;

import cn.richie.ffmpeg.FFmpegNative;

public class FFmpegVideoActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(FFmpegVideoActivity.class);
    private FFmpegNative mFFmpegNative;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_video);
        mFFmpegNative = new FFmpegNative();
        SurfaceView surfaceView = findViewById(R.id.video_surface);
        File dir = FileUtils.getExternalAssetsDir(FFmpegVideoActivity.this);
        final File videoFile = new File(dir, "sample.mp4");
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                logger.debug("surfaceCreated() called with: holder = [" + holder + "]");
                mFFmpegNative.createPlayer(videoFile.getAbsolutePath(), holder.getSurface());
                mFFmpegNative.play();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                logger.debug("surfaceChanged() called with: holder = [" + holder + "], format = [" + format + "], width = [" + width + "], height = [" + height + "]");
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                logger.debug("surfaceDestroyed() called with: holder = [" + holder + "]");
                mFFmpegNative.pause();
                mFFmpegNative.releasePlayer();
            }
        });

    }
}