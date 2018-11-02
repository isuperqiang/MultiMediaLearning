package com.richie.multimedialearning.camera;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.TextureView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;

/**
 * @author Richie on 2018.10.27
 * 使用 TextureView 预览
 */
public class CameraTexturePreview extends TextureView implements TextureView.SurfaceTextureListener {
    private final ILogger logger = LoggerFactory.getLogger(CameraTexturePreview.class);
    private Camera mCamera;
    private Activity mActivity;

    public CameraTexturePreview(Context context) {
        super(context);
        init();
    }

    public CameraTexturePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraTexturePreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setSurfaceTextureListener(this);
        mActivity = (Activity) getContext();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        logger.debug("onSurfaceTextureAvailable. width:{}, height:{}", width, height);
        openCamera();
        startPreviewDisplay(surface);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        logger.debug("onSurfaceTextureSizeChanged. width:{}, height:{}", width, height);
        // Ignored, Camera does all the work for us
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        logger.debug("onSurfaceTextureDestroyed");
        releaseCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Invoked every time there's a new Camera preview frame
    }

    private void openCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int number = Camera.getNumberOfCameras();
        logger.info("camera number:{}", number);
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(mActivity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                } catch (Exception e) {
                    logger.error("openCamera", e);
                    mActivity.onBackPressed();
                }
                break;
            }
        }
    }

    private void startPreviewDisplay(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                logger.error("Error while START preview for camera", e);
            }
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(null);
                mCamera.release();
            } catch (IOException e) {
                logger.error("releaseCamera: ", e);
            }
            mCamera = null;
        }
    }

}
