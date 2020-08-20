package com.richie.multimedialearning.muxerextract;

import android.app.Activity;
import android.content.Context;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.CameraUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.IOException;

/**
 * 使用 SurfaceView 预览
 *
 * @author Richie on 2018.08.01
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Camera.PreviewCallback {
    private final ILogger logger = LoggerFactory.getLogger(CameraSurfaceView.class);
    private SurfaceHolder mSurfaceHolder;
    private Camera mCamera;
    private Activity mActivity;
    private int mPreviewWidth = 1280;
    private int mPreviewHeight = 720;
    private int mFrameRate = 30;
    private H264Encoder mH264Encoder;
    private byte[] mByteCopy;

    public CameraSurfaceView(Context context) {
        this(context, null);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CameraSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
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
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                openCamera();
                startPreviewDisplay();
                mH264Encoder = new H264Encoder(mPreviewWidth, mPreviewHeight, mFrameRate);
                mH264Encoder.startEncoder();
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logger.debug("surfaceChanged: format:{}, width:{}, height:{}", format, width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        logger.debug("surfaceDestroyed");
        ThreadHelper.getInstance().runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                mH264Encoder.stopEncoder();
                releaseCamera();
            }
        });
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
                    CameraUtils.choosePreviewSize(params, mPreviewWidth, mPreviewHeight);
                    CameraUtils.chooseFrameRate(params, mFrameRate);
                    CameraUtils.setFocusModes(params);
                    mCamera.setParameters(params);
                } catch (Exception e) {
                    logger.error("openCamera error", e);
                }
                break;
            }
        }
    }

    private void startPreviewDisplay() {
        logger.debug("startPreviewDisplay");
        if (mCamera != null) {
            try {
                mCamera.setPreviewDisplay(mSurfaceHolder);
                mCamera.setPreviewCallbackWithBuffer(this);
                int length = mPreviewWidth * mPreviewHeight * ImageFormat.getBitsPerPixel(ImageFormat.NV21) / 8;
                mByteCopy = new byte[length];
                byte[][] buffer = new byte[3][length];
                for (byte[] bytes : buffer) {
                    mCamera.addCallbackBuffer(bytes);
                }
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
                mCamera.setPreviewDisplay(null);
                mCamera.setPreviewCallbackWithBuffer(null);
                mCamera.release();
            } catch (IOException e) {
                logger.error("releaseCamera error", e);
            }
            mCamera = null;
        }
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        logger.debug("onPreviewFrame: {}", data);
        System.arraycopy(data, 0, mByteCopy, 0, data.length);
        mH264Encoder.putData(mByteCopy);
    }

}
