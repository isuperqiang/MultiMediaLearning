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
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.IOException;

/**
 * 使用 TextureView 预览相机画面
 *
 * @author Richie on 2018.10.27
 */
public class CameraTexturePreview extends TextureView implements TextureView.SurfaceTextureListener {
    private final ILogger logger = LoggerFactory.getLogger(CameraTexturePreview.class);
    private Camera mCamera;
    private Activity mActivity;

    public CameraTexturePreview(Context context) {
        this(context, null);
    }

    public CameraTexturePreview(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
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
    public void onSurfaceTextureAvailable(final SurfaceTexture surface, int width, int height) {
        logger.info("onSurfaceTextureAvailable. width:{}, height:{}", width, height);
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                openCamera();
                // 异步打开相机，创建预览画面
                startPreviewDisplay(surface);
            }
        });
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        logger.info("onSurfaceTextureSizeChanged. width:{}, height:{}", width, height);
        // Ignored, Camera does all the work for us
        surface.setDefaultBufferSize(CameraUtils.PREVIEW_HEIGHT, CameraUtils.PREVIEW_WIDTH);
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        logger.info("onSurfaceTextureDestroyed");
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                releaseCamera();
            }
        });
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // called every time there's a new Camera preview frame
    }

    private void openCamera() {
        logger.debug("openCamera");
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int number = Camera.getNumberOfCameras();
        for (int i = 0; i < number; i++) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                try {
                    mCamera = Camera.open(i);
                    CameraUtils.setCameraDisplayOrientation(mActivity, i, mCamera);
                    Camera.Parameters params = mCamera.getParameters();
                    params.setPreviewSize(CameraUtils.PREVIEW_WIDTH, CameraUtils.PREVIEW_HEIGHT);
                    CameraUtils.chooseFrameRate(params, 30);
                    CameraUtils.setFocusModes(params);
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    logger.error("openCamera error", e);
                }
                break;
            }
        }
    }

    private void startPreviewDisplay(SurfaceTexture surfaceTexture) {
        if (mCamera != null) {
            try {
                // 设置 NV21 数据回调，发生在 worker 线程
                mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        logger.verbose("onPreviewFrame data length:{}", data != null ? data.length : 0);
                    }
                });
                mCamera.setPreviewTexture(surfaceTexture);
                mCamera.startPreview();
            } catch (IOException e) {
                logger.error("startPreviewDisplay error", e);
            }
        }
    }

    private void releaseCamera() {
        logger.debug("releaseCamera");
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewTexture(null);
                mCamera.release();
            } catch (IOException e) {
                logger.error("releaseCamera error", e);
            }
            mCamera = null;
        }
    }

}
