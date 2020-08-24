package com.richie.multimedialearning.opengl;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;

import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;

/**
 * 相机管理类
 *
 * @author Richie on 2018.08.01
 */
public class CameraHolder {
    private static final String TAG = "CameraHolder";
    private static final int BUFFER_COUNT = 3;
    private static final int PREVIEW_WIDTH = 1280;
    private static final int PREVIEW_HEIGHT = 720;
    private Camera mCamera;
    private int mPreviewWidth;
    private int mPreviewHeight;

    public void setPreviewTexture(SurfaceTexture texture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(texture);
            } catch (IOException e) {
                Log.e(TAG, "setPreviewTexture: ", e);
            }
        }
    }

    public void setOnPreviewFrameCallback(final PreviewFrameCallback callback) {
        if (mCamera != null) {
            for (int i = 0; i < BUFFER_COUNT; i++) {
                byte[] buffer = new byte[mPreviewWidth * mPreviewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
                mCamera.addCallbackBuffer(buffer);
            }
            mCamera.setPreviewCallbackWithBuffer(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    camera.addCallbackBuffer(data);
                    callback.onPreviewFrame(data);
                }
            });
        }
    }

    public void startPreview() {
        Log.d(TAG, "startPreview: ");
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public void release() {
        Log.d(TAG, "release: ");
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
            } catch (IOException e) {
                Log.e(TAG, "release: ", e);
            }
            mCamera = null;
        }
    }

    public void openCamera(Activity activity) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0, number = Camera.getNumberOfCameras(); i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(activity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    int[] previewSize = CameraUtils.choosePreviewSize(params, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    mPreviewWidth = previewSize[0];
                    mPreviewHeight = previewSize[1];
                    CameraUtils.setFocusModes(params);
                    CameraUtils.chooseFrameRate(params, 30);
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    Log.e(TAG, "openCamera: ", e);
                }
                break;
            }
        }
    }

    public void stopPreview() {
        Log.d(TAG, "stopPreview: ");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    public int getPreviewWidth() {
        return mPreviewWidth;
    }

    public int getPreviewHeight() {
        return mPreviewHeight;
    }

    public interface PreviewFrameCallback {
        /**
         * camera preview frame data
         *
         * @param bytes
         */
        void onPreviewFrame(byte[] bytes);
    }

}
