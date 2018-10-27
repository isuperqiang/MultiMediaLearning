package com.richie.multimedialearning.camera;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;

import java.io.IOException;

/**
 * @author Richie on 2018.08.01
 * 使用 SurfaceView 预览
 */
public class CameraSurfacePreview extends SurfaceView implements SurfaceHolder.Callback {
    private final ILogger logger = LoggerFactory.getLogger(CameraSurfacePreview.class);
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Activity mActivity;

    public CameraSurfacePreview(Context context) {
        super(context);
        init();
    }

    public CameraSurfacePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CameraSurfacePreview(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mActivity = (Activity) getContext();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        logger.debug("surfaceCreated");
        openCamera();
        startPreviewDisplay();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logger.debug("surfaceChanged: format:{}, width:{}, height:{}", format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        logger.debug("surfaceDestroyed");
        releaseCamera();
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
                    logger.error("openCamera: ", e);
                    mActivity.onBackPressed();
                }
                break;
            }
        }
    }

    private void startPreviewDisplay() {
        checkCamera();
        try {
            mCamera.setPreviewDisplay(mSurfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            logger.error("Error while START preview for camera", e);
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewDisplay(null);
                mCamera.release();
            } catch (IOException e) {
                logger.error("releaseCamera: ", e);
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
