package com.richie.multimedialearning.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.TextureView;

import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;

/**
 * @author Richie on 2018.10.27
 * 使用 TextureView 预览
 */
public class CameraTexPreview extends TextureView implements TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraTexPreview";
    private Camera mCamera;
    private Activity mActivity;

    public CameraTexPreview(Context context) {
        super(context);
        init();
    }

    public CameraTexPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraTexPreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureAvailable. width:" + width + ", height:" + height);
        openCamera();
        startPreviewDisplay(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        Log.d(TAG, "onSurfaceTextureSizeChanged. width:" + width + ", height:" + height);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed");
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // 绘制每帧时调用
        //Log.d(TAG, "onSurfaceTextureUpdated");
    }

    private void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int number = Camera.getNumberOfCameras();
        Log.i(TAG, "camera number:" + number);
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(mActivity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } catch (Exception e) {
                    Log.e(TAG, "openCamera: ", e);
                    mActivity.onBackPressed();
                }
                break;
            }
        }
    }

    private void startPreviewDisplay(SurfaceTexture surfaceTexture) {
        checkCamera();
        try {
            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "Error while START preview for camera", e);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(null);
                mCamera.release();
            } catch (IOException e) {
                Log.e(TAG, "releaseCamera: ", e);
            }
            mCamera = null;
        }
    }

    private void checkCamera() {
        if (mCamera == null) {
            throw new IllegalStateException("Camera must be set when start/stop preview, call <setCamera(Camera)> to set");
        }
    }
}
