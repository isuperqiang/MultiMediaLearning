package com.richie.multimedialearning.mediacodec;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.richie.multimedialearning.R;

public class CameraOpenGlActivity extends AppCompatActivity {

    private CameraRenderer mRenderer;
    private GLSurfaceView mGlSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_open_gl);
        mGlSurfaceView = findViewById(R.id.gl_surface);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mRenderer = new CameraRenderer(this);
        mGlSurfaceView.setRenderer(mRenderer);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGlSurfaceView.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mGlSurfaceView.onPause();
        mRenderer.onStop();
    }
}
