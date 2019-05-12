package com.richie.multimedialearning.mediacodec;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.widget.Toast;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.opengl.CameraHolder;
import com.richie.multimedialearning.opengl.GLESUtils;
import com.richie.multimedialearning.utils.BitmapUtils;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.LimitFpsUtil;
import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.program.TextureProgram;

import java.io.File;
import java.io.IOException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2019.05.05
 */
public class CameraRenderer implements GLSurfaceView.Renderer {
    private final ILogger logger = LoggerFactory.getLogger(CameraRenderer.class);
    private int mTextureID;
    private float[] mMvpMatrix = new float[16];
    private float[] mTexMatrix = new float[16];
    private SurfaceTexture mSurfaceTexture;
    private CameraHolder mCameraHolder;
    private Activity mActivity;
    private GLSurfaceView mGLSurfaceView;
    private TextureProgram mTextureProgram;
    private volatile boolean mCallSnapshot;

    public CameraRenderer(Activity activity, GLSurfaceView glSurfaceView) {
        mActivity = activity;
        mGLSurfaceView = glSurfaceView;
        mCameraHolder = new CameraHolder();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        logger.debug("onSurfaceCreated() called");
        GlUtil.logVersionInfo();
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        mTextureProgram = TextureProgram.createTextureOES();
        mTextureID = GLESUtils.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
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
        mTextureProgram.drawFrame(mTextureID, mTexMatrix, mMvpMatrix);
        snapshot();
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
                mTextureProgram.release();
            }
        });
        mCameraHolder.stopPreview();
        mCameraHolder.release();
    }

    public void setSnapshot() {
        mCallSnapshot = true;
    }

    private void snapshot() {
        if (mCallSnapshot) {
            logger.debug("snapshot() called");
            mCallSnapshot = false;
            GLESUtils.glReadBitmap(mTextureID, mTexMatrix, GLESUtils.IDENTITY_MATRIX, mCameraHolder.getPreviewSize().y,
                    mCameraHolder.getPreviewSize().x, mTextureProgram, new GLESUtils.OnReadBitmapListener() {
                        @Override
                        public void onReadBitmap(Bitmap bitmap) {
                            logger.debug("onReadBitmap. {}", bitmap);
                            File dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                            File dest = new File(new File(dcim, "Camera"), FileUtils.getUUID32() + ".jpg");
                            try {
                                BitmapUtils.writeBitmapLocal(bitmap, dest);
                                logger.info("write finish: {}", dest);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, "保存成功", Toast.LENGTH_SHORT).show();
                                        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest)));
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    });
        }
    }
}
