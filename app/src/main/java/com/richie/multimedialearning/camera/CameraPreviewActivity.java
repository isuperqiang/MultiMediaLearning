package com.richie.multimedialearning.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.BarUtils;

/**
 * SurfaceView 预览相机画面
 */
public class CameraPreviewActivity extends AppCompatActivity {

    public static final String PREVIEW_TYPE = "preview_type";
    public static final int TYPE_SURFACE_VIEW = 902;
    public static final int TYPE_TEXTURE_VIEW = 203;
    private static final int PERM = 212;
    private int mPreviewType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        BarUtils.setStatusBarVisibility(this, false);

        mPreviewType = getIntent().getIntExtra(PREVIEW_TYPE, TYPE_SURFACE_VIEW);
        if (mPreviewType == TYPE_SURFACE_VIEW) {
            Toast.makeText(this, "By SurfaceView", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "By TextureView", Toast.LENGTH_SHORT).show();
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERM);
        } else {
            // has permission
            previewCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERM && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            previewCamera();
        }
    }

    private void previewCamera() {
        ConstraintLayout layout = findViewById(R.id.cl_root);
        if (mPreviewType == TYPE_SURFACE_VIEW) {
            CameraSurfacePreview cameraPreview = new CameraSurfacePreview(this);
            layout.addView(cameraPreview);
        } else {
            CameraTexturePreview cameraPreview = new CameraTexturePreview(this);
            layout.addView(cameraPreview);
        }
    }

}
