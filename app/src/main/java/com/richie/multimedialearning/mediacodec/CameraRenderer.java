package com.richie.multimedialearning.mediacodec;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.opengl.CameraHolder;
import com.richie.multimedialearning.opengl.GLESUtils;
import com.richie.multimedialearning.utils.LimitFpsUtil;
import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.program.ProgramTextureOES;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2019.05.05
 */
public class CameraRenderer implements GLSurfaceView.Renderer {
    private final ILogger logger = LoggerFactory.getLogger(CameraRenderer.class);
    // 纹理句柄
    private int mTextureID;
    // 顶点坐标 MVP 变换矩阵
    private float[] mMvpMatrix = new float[16];
    // 纹理坐标变换矩阵
    private float[] mTexMatrix = new float[16];
    // 回调数据使用的buffer索引
    private SurfaceTexture mSurfaceTexture;
    private CameraHolder mCameraHolder;
    private Activity mActivity;
    private GLSurfaceView mGLSurfaceView;
    private ProgramTextureOES mProgramTextureOES;

    public CameraRenderer(Activity activity, GLSurfaceView glSurfaceView) {
        mActivity = activity;
        mGLSurfaceView = glSurfaceView;
        mCameraHolder = new CameraHolder();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        logger.debug("onSurfaceCreated() called");
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        mProgramTextureOES = new ProgramTextureOES();
        mTextureID = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        logger.debug("onSurfaceChanged() called. width:{}, height:{}", width, height);
        GLES20.glViewport(0, 0, width, height);
        Point previewSize = mCameraHolder.getPreviewSize();
        mMvpMatrix = GLESUtils.changeMvpMatrixCrop(width, height, previewSize.y, previewSize.x);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTexMatrix);

        mProgramTextureOES.drawFrame(mTextureID, mTexMatrix, mMvpMatrix);

        LimitFpsUtil.limitFrameRate(30);
    }

    public void onResume() {
        mCameraHolder.openCamera(mActivity);
        mCameraHolder.setOnPreviewFrameCallback(new CameraHolder.PreviewFrameCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes) {
                //logger.verbose("onPreviewFrame, byteLength:{}", bytes.length);
                mGLSurfaceView.requestRender();
            }
        });
    }

    private void startPreview() {
        if (mSurfaceTexture != null) {
            mSurfaceTexture.release();
        }
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        mCameraHolder.setPreviewTexture(mSurfaceTexture);
        mCameraHolder.startPreview();
    }

    public void onStop() {
        mGLSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mProgramTextureOES.release();
            }
        });
        mCameraHolder.stopPreview();
        mCameraHolder.release();
    }

}
