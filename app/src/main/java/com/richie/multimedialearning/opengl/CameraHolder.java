package com.richie.multimedialearning.opengl;


import android.app.Activity;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Richie on 2018.08.01
 */
public class CameraHolder {
    public static final int PREVIEW_WIDTH = 1280;
    public static final int PREVIEW_HEIGHT = 720;
    private final ILogger logger = LoggerFactory.getLogger(getClass());
    private Camera mCamera;
    private Point mPreviewSize;

    public void setPreviewTexture(SurfaceTexture texture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(texture);
            } catch (IOException e) {
                logger.error(e);
            }
        }
    }

    public void setOnPreviewFrameCallback(final PreviewFrameCallback callback) {
        if (mCamera != null) {
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    callback.onPreviewFrame(data, mPreviewSize.x, mPreviewSize.y);
                }
            });
        }
    }

    public void startPreview() {
        logger.debug("startPreview() called");
        if (mCamera != null) {
            mCamera.startPreview();
        }
    }

    public void release() {
        logger.debug("release() called");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public Point getPreviewSize() {
        return mPreviewSize;
    }

    public void openCamera(Activity activity) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int number = Camera.getNumberOfCameras();
        logger.info("openCamera camera number:{}", number);
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(activity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    int[] previewSize = CameraUtils.choosePreviewSize(params, PREVIEW_WIDTH, PREVIEW_HEIGHT);
                    logger.info("previewSize:{}", Arrays.toString(previewSize));
                    params.setPreviewSize(previewSize[0], previewSize[1]);
                    mPreviewSize = new Point(previewSize[0], previewSize[1]);
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    logger.error("openCamera error", e);
                }
                break;
            }
        }
    }

    public void stopPreview() {
        logger.debug("stopPreview() called");
        if (mCamera != null) {
            mCamera.stopPreview();
        }
    }

    interface PreviewFrameCallback {
        /**
         * 预览帧
         *
         * @param bytes
         * @param width  图像的宽度
         * @param height 图像的高度
         */
        void onPreviewFrame(byte[] bytes, int width, int height);
    }

}
