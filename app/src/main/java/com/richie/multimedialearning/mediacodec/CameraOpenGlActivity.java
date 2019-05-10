package com.richie.multimedialearning.mediacodec;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.multimedialearning.R;

public class CameraOpenGlActivity extends AppCompatActivity implements View.OnClickListener {

    private CameraRenderer mCameraRenderer;
    private GLSurfaceView mGlSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_open_gl);
        mGlSurfaceView = findViewById(R.id.gl_surface);
        mGlSurfaceView.setEGLContextClientVersion(2);
        mCameraRenderer = new CameraRenderer(this, mGlSurfaceView);
        mGlSurfaceView.setRenderer(mCameraRenderer);
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        findViewById(R.id.btn_take_photo).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGlSurfaceView.onResume();
        mCameraRenderer.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraRenderer.onStop();
        mGlSurfaceView.onPause();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_photo: {
                mCameraRenderer.setSnapshot();
            }
            break;
            default:
        }
    }
}
