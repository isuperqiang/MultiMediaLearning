package com.richie.multimedialearning.mediacodec;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.opengl.GLESUtils;

/**
 * 使用 OpenGL 预览相机画面，并用 MediaCodec 编码 H264
 *
 * @author Richie on 2018.10.22
 */
public class CameraOpenGlActivity extends AppCompatActivity implements View.OnClickListener {
    private CameraRenderer mCameraRenderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_open_gl);
        GLSurfaceView glSurfaceView = findViewById(R.id.gl_surface);
        glSurfaceView.setEGLContextClientVersion(GLESUtils.getSupportGLVersion(this));
        mCameraRenderer = new CameraRenderer(this, glSurfaceView);
        glSurfaceView.setRenderer(mCameraRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        findViewById(R.id.btn_take_photo).setOnClickListener(this);
        findViewById(R.id.btn_record_video).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCameraRenderer.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraRenderer.onStop();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_take_photo: {
                mCameraRenderer.setTakePhoto();
            }
            break;
            case R.id.btn_record_video: {
                boolean started = v.getTag() != null && (boolean) v.getTag();
                ((Button) v).setText(started ? "录像" : "停止");
                v.setTag(!started);
                if (started) {
                    // stop
                    mCameraRenderer.setRecordVideo(false);
                } else {
                    // start
                    mCameraRenderer.setRecordVideo(true);
                }
            }
            break;
            default:
        }
    }
}
