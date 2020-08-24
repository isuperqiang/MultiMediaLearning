package com.richie.multimedialearning.mediacodec;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.opengl.EGL14;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.richie.multimedialearning.opengl.CameraHolder;
import com.richie.multimedialearning.opengl.GLESUtils;
import com.richie.multimedialearning.utils.BitmapUtils;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.LimitFpsUtil;
import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.program.TextureProgram;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2019.05.05
 */
public class CameraRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "CameraRenderer";
    private int mTextureId;
    private float[] mMvpMatrix = new float[16];
    private float[] mTexMatrix = new float[16];
    private SurfaceTexture mSurfaceTexture;
    private CameraHolder mCameraHolder;
    private Activity mActivity;
    private GLSurfaceView mGlSurfaceView;
    private TextureProgram mTextureProgram;
    private volatile boolean mCallTakePhoto;
    private volatile CameraSurfaceCodec mCameraSurfaceCodec;

    public CameraRenderer(Activity activity, GLSurfaceView glSurfaceView) {
        mActivity = activity;
        mGlSurfaceView = glSurfaceView;
        mCameraHolder = new CameraHolder();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated: ");
        GlUtil.logVersionInfo();
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        mTextureProgram = TextureProgram.createTextureOES();
        mTextureId = GLESUtils.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mSurfaceTexture = new SurfaceTexture(mTextureId);
        mCameraHolder.setPreviewTexture(mSurfaceTexture);
        mCameraHolder.startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(TAG, "onSurfaceChanged() width = [" + width + "], height = [" + height + "]");
        GLES20.glViewport(0, 0, width, height);
        mMvpMatrix = GLESUtils.changeMvpMatrixCrop(width, height, mCameraHolder.getPreviewHeight(),
                mCameraHolder.getPreviewWidth());
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTexMatrix);
        mTextureProgram.drawFrame(mTextureId, mTexMatrix, mMvpMatrix);
        takePhoto();
        recordVideo();
        LimitFpsUtil.limitFrameRate(30);
    }

    public void onResume() {
        mCameraHolder.openCamera(mActivity);
        mCameraHolder.setOnPreviewFrameCallback(new CameraHolder.PreviewFrameCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes) {
                //logger.verbose("onPreviewFrame, byte length:{}", bytes.length);
                mGlSurfaceView.requestRender();
            }
        });
        mGlSurfaceView.onResume();
    }

    public void onStop() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mGlSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                mTextureProgram.release();
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            // ignored
        }
        mGlSurfaceView.onPause();
        mCameraHolder.stopPreview();
        mCameraHolder.release();
    }

    public void setRecordVideo(boolean run) {
        if (run) {
            CameraSurfaceCodec cameraSurfaceCodec = new CameraSurfaceCodec();
            cameraSurfaceCodec.setOnEncodeListener(new CameraSurfaceCodec.OnEncodeListener() {
                @Override
                public void onPrepare() {
                    Log.d(TAG, "onPrepare: prepare");
                    mGlSurfaceView.queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            cameraSurfaceCodec.prepare(EGL14.eglGetCurrentContext());
                        }
                    });
                }

                @Override
                public void onStop() {

                }
            });
            File outFile = new File(FileUtils.getAvcFileDir(mActivity), FileUtils.getUUID32() + ".mp4");
            cameraSurfaceCodec.create(mCameraHolder.getPreviewHeight(), mCameraHolder.getPreviewWidth(), outFile.getAbsolutePath());
            mCameraSurfaceCodec = cameraSurfaceCodec;
        } else {
            mCameraSurfaceCodec.release();
            mCameraSurfaceCodec = null;
        }
    }

    private void recordVideo() {
        if (mCameraSurfaceCodec != null) {
            mCameraSurfaceCodec.draw(mTextureId, mTexMatrix, GlUtil.IDENTITY_MATRIX);
        }
    }

    public void setTakePhoto() {
        mCallTakePhoto = true;
    }

    private void takePhoto() {
        if (mCallTakePhoto) {
            mCallTakePhoto = false;
            GLESUtils.glReadBitmap(mTextureId, mTexMatrix, GLESUtils.IDENTITY_MATRIX, mCameraHolder.getPreviewHeight(),
                    mCameraHolder.getPreviewWidth(), mTextureProgram, new GLESUtils.OnReadBitmapListener() {
                        @Override
                        public void onReadBitmap(Bitmap bitmap) {
                            File dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                            File dest = new File(new File(dcimDir, "Camera"), FileUtils.getUUID32() + ".jpg");
                            try {
                                BitmapUtils.writeBitmapLocal(bitmap, dest);
                                Log.i(TAG, "save bitmap finish: " + dest);
                                mActivity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(mActivity, "保存成功", Toast.LENGTH_SHORT).show();
                                        mActivity.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(dest)));
                                    }
                                });
                            } catch (IOException e) {
                                Log.e(TAG, "onReadBitmap: ", e);
                            }
                        }
                    });
        }
    }

}
