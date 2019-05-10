package com.richie.multimedialearning.opengl;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;
import java.util.Arrays;

/**
 * 相机管理类
 *
 * @author Richie on 2018.08.01
 */
public class CameraHolder {
    private static final int BUFFER_COUNT = 4;
    private static final int PREVIEW_WIDTH = 1920;
    private static final int PREVIEW_HEIGHT = 1080;
    private final ILogger logger = LoggerFactory.getLogger(CameraHolder.class);
    private Camera mCamera;
    // preview size: width and height
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
            for (int i = 0; i < BUFFER_COUNT; i++) {
                byte[] buffer = new byte[mPreviewSize.x * mPreviewSize.y * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8];
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
        logger.info("openCamera camera numbers:{}", number);
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
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

    public interface PreviewFrameCallback {
        /**
         * camera preview frame data
         *
         * @param bytes
         */
        void onPreviewFrame(byte[] bytes);
    }

}
