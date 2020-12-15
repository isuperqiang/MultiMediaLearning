package com.richie.multimedialearning.camera;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.BarUtils;

/**
 * 预览相机画面
 */
public class CameraPreviewActivity extends AppCompatActivity {
    public static final String PREVIEW_TYPE = "preview_type";
    public static final int TYPE_SURFACE_VIEW_CAMERA = 902;
    public static final int TYPE_SURFACE_VIEW_CAMERA2 = 932;
    public static final int TYPE_TEXTURE_VIEW_CAMERA = 203;
    private int mPreviewType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        BarUtils.setStatusBarVisibility(this, false);

        mPreviewType = getIntent().getIntExtra(PREVIEW_TYPE, TYPE_SURFACE_VIEW_CAMERA);
        previewCamera();
    }

    private void previewCamera() {
        ConstraintLayout layout = findViewById(R.id.cl_root);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(0, 0);
        // 避免相机画面被拉伸
        params.dimensionRatio = "9:16";
        params.leftToLeft = ConstraintLayout.LayoutParams.PARENT_ID;
        params.rightToRight = ConstraintLayout.LayoutParams.PARENT_ID;
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        if (mPreviewType == TYPE_SURFACE_VIEW_CAMERA) {
            CameraSurfacePreview cameraPreview = new CameraSurfacePreview(this);
            layout.addView(cameraPreview, params);
        } else if (mPreviewType == TYPE_TEXTURE_VIEW_CAMERA) {
            CameraTexturePreview cameraPreview = new CameraTexturePreview(this);
            layout.addView(cameraPreview, params);
        }
    }

}
