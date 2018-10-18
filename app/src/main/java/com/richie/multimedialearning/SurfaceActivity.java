package com.richie.multimedialearning;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;

/**
 * SurfaceView
 */
public class SurfaceActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(SurfaceActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface);
        SurfaceView surfaceView = findViewById(R.id.surface_view);
        SurfaceViewCallback surfaceViewCallback = new SurfaceViewCallback();
        surfaceView.getHolder().addCallback(surfaceViewCallback);
    }

    private class SurfaceViewCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            logger.debug("onSurfaceCreated");

        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            logger.debug("onSurfaceChanged. format:{}, width:{}, height:{}", format, width, height);
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            logger.debug("onSurfaceDestroyed");
        }
    }
}
