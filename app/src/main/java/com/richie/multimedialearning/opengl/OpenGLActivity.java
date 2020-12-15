package com.richie.multimedialearning.opengl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;

/**
 * 使用 OpenGL 绘制
 */
public class OpenGLActivity extends AppCompatActivity {
    public static final String TYPE = "draw_type";
    public static final int TYPE_TRIANGLE = 140;
    public static final int TYPE_IMAGE = 100;
    private GLSurfaceView mGlSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_gl);
        int glVersion = GLESUtils.getSupportGLVersion(this);
        String msg = "支持 GLES " + glVersion;
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        mGlSurfaceView = findViewById(R.id.gl_surface);
        // Create an OpenGL ES 2.0 context
        mGlSurfaceView.setEGLContextClientVersion(2);
        GLSurfaceView.Renderer renderer = null;
        int type = getIntent().getIntExtra(TYPE, TYPE_IMAGE);
        if (type == TYPE_IMAGE) {
            File imageFile = new File(FileUtils.getExternalAssetsDir(this), "sample.jpg");
            renderer = new ImageRenderer(imageFile.getAbsolutePath());
        } else {
            renderer = new TriangleRenderer();
        }
        // Set the Renderer for drawing on the GLSurfaceView
        mGlSurfaceView.setRenderer(renderer);
        // Render the view only when there is a change in the drawing data
        mGlSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
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
    }

}
