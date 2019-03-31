package com.richie.multimedialearning.camera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.BarUtils;

/**
 * 预览相机画面
 */
public class CameraPreviewActivity extends AppCompatActivity {

    public static final String PREVIEW_TYPE = "preview_type";
    public static final int TYPE_SURFACE_VIEW_CAMERA = 902;
    public static final int TYPE_SURFACE_VIEW_CAMER2 = 932;
    public static final int TYPE_TEXTURE_VIEW_CAMERA = 203;
    public static final int TYPE_TEXTURE_VIEW_CAMER2 = 213;
    private static final int PERM = 212;
    private int mPreviewType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        BarUtils.setStatusBarVisibility(this, false);

        mPreviewType = getIntent().getIntExtra(PREVIEW_TYPE, TYPE_SURFACE_VIEW_CAMERA);
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
        if (mPreviewType == TYPE_SURFACE_VIEW_CAMERA) {
            CameraSurfacePreview cameraPreview = new CameraSurfacePreview(this);
            layout.addView(cameraPreview);
        } else if (mPreviewType == TYPE_TEXTURE_VIEW_CAMERA) {
            CameraTexturePreview cameraPreview = new CameraTexturePreview(this);
            layout.addView(cameraPreview);
        } else if (mPreviewType == TYPE_SURFACE_VIEW_CAMER2) {
            Camera2SurfacePreview cameraPreview = new Camera2SurfacePreview(this);
            layout.addView(cameraPreview);
            //} else if (mPreviewType == TYPE_TEXTURE_VIEW_CAMER2) {

        }
    }

}
