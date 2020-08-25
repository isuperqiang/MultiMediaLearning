package com.richie.multimedialearning.mediacodec;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.Surface;

import com.richie.multimedialearning.utils.gles.EglCore;
import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.OffscreenSurface;
import com.richie.multimedialearning.utils.gles.program.TextureProgram;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * mp4 视频解码，保存 RGBA 图片
 *
 * @author Richie on 2020.08.22
 */
public class AvcRgbaCodec implements SurfaceTexture.OnFrameAvailableListener {
    private static final String TAG = "AvcRgbaCodec";
    private static final boolean VERBOSE = true;
    private EglCore mEglCore;
    private OffscreenSurface mOffscreenSurface;
    private ByteBuffer mByteBuffer;
    private int mWidth;
    private int mHeight;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private int mTexId;
    private TextureProgram mTextureProgram;
    private float[] mTransformMatrix = new float[16];
    private int mFrames;
    private File mDestDir;
    private final Object mLock = new Object();
    private Handler mSurfaceHandler;

    public void decode(final File src, final File destDir) throws IOException {
        mDestDir = destDir;
        MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(src.getAbsolutePath());
        MediaFormat mediaFormat = null;
        for (int i = 0, count = extractor.getTrackCount(); i < count; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if (mime.startsWith("video/")) {
                extractor.selectTrack(i);
                mediaFormat = format;
                break;
            }
        }
        if (mediaFormat == null) {
            Log.e(TAG, "Invalid file with video track.");
            extractor.release();
            return;
        }

        final int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
        final int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
        String mime = mediaFormat.getString(MediaFormat.KEY_MIME);
        Log.i(TAG, "mime: " + mime + ", width: " + width + ", height: " + height);

        HandlerThread handlerThread = new HandlerThread("video_decoder");
        handlerThread.start();
        mSurfaceHandler = new Handler(handlerThread.getLooper());
        CountDownLatch countDownLatch = new CountDownLatch(1);
        mSurfaceHandler.post(new Runnable() {
            @Override
            public void run() {
                createEgl(width, height);
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        MediaCodec codec = MediaCodec.createDecoderByType(mime);
        codec.configure(mediaFormat, mSurface, null, 0);
        codec.start();

        ByteBuffer[] inputBuffers = codec.getInputBuffers();
        MediaCodec.BufferInfo outBufferInfo = new MediaCodec.BufferInfo();
        final long timeoutUs = 10_000;
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;
        int inputChunk = 0;
        try {
            while (!sawOutputEOS) {
                if (!sawInputEOS) {
                    int inputBufIndex = codec.dequeueInputBuffer(timeoutUs);
                    if (inputBufIndex >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufIndex];
                        int chunkSize = extractor.readSampleData(inputBuffer, 0);
                        if (chunkSize < 0) {
                            sawInputEOS = true;
                            codec.queueInputBuffer(inputBufIndex, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            Log.i(TAG, "saw input EOS");
                        } else {
                            codec.queueInputBuffer(inputBufIndex, 0, chunkSize, extractor.getSampleTime(), 0);
                            inputChunk++;
                            if (VERBOSE) {
                                Log.v(TAG, "submitted frame " + inputChunk + " to decoder, size " + chunkSize);
                            }
                            extractor.advance();
                        }
                    }
                }

                int outBufIndex = codec.dequeueOutputBuffer(outBufferInfo, timeoutUs);
                if (outBufIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    Log.d(TAG, "no output from decoder available");
                } else if (outBufIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    Log.d(TAG, "decoder output buffers changed");
                } else if (outBufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = codec.getOutputFormat();
                    Log.d(TAG, "decoder output format changed: " + newFormat);
                } else if (outBufIndex < 0) {
                    Log.w(TAG, "unexpected result from decoder.dequeueOutputBuffer: " + outBufferInfo);
                } else { // decoderStatus >= 0
                    if (VERBOSE) {
                        Log.v(TAG, "surface decoder given buffer " + outBufIndex + ", size " + outBufferInfo.size);
                    }
                    if ((outBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        Log.i(TAG, "saw output EOS");
                        sawOutputEOS = true;
                    }
                    boolean doRender = outBufferInfo.size != 0;
                    codec.releaseOutputBuffer(outBufIndex, doRender);

                    synchronized (mLock) {
                        mLock.notify();
                        if (!sawOutputEOS) {
                            lockWait(mLock);
                        }
                    }
                }
            }
        } finally {
            Log.i(TAG, "decode finish " + destDir.getAbsolutePath());
            mSurfaceHandler.post(new Runnable() {
                @Override
                public void run() {
                    releaseEgl();
                }
            });
            mSurfaceHandler.getLooper().quitSafely();
            codec.stop();
            codec.release();
            extractor.release();
        }
    }

    private void createEgl(int width, int height) {
        Log.d(TAG, "create egl. width " + width + ", height " + height);
        mWidth = width;
        mHeight = height;
        mByteBuffer = ByteBuffer.allocateDirect(width * height * 4);
        mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
        mEglCore = new EglCore();
        mOffscreenSurface = new OffscreenSurface(mEglCore, width, height);
        mOffscreenSurface.makeCurrent();
        mTexId = GlUtil.createTextureObject(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        mTextureProgram = TextureProgram.createTextureOES();
        mSurfaceTexture = new SurfaceTexture(mTexId);
        mSurface = new Surface(mSurfaceTexture);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mSurfaceTexture.setOnFrameAvailableListener(this, mSurfaceHandler);
        } else {
            mSurfaceTexture.setOnFrameAvailableListener(this);
        }
    }

    private void releaseEgl() {
        Log.d(TAG, "release egl");
        if (mTexId > 0) {
            GlUtil.deleteTextures(new int[]{mTexId});
            mTexId = -1;
        }
        mTextureProgram.release();
        mTextureProgram = null;
        mSurface.release();
        mSurface = null;
        mSurfaceTexture.setOnFrameAvailableListener(null);
        mSurfaceTexture.release();
        mSurfaceTexture = null;
        mOffscreenSurface.release();
        mOffscreenSurface = null;
        mEglCore.release();
        mEglCore = null;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        mFrames++;
        if (VERBOSE) {
            Log.v(TAG, "onFrameAvailable: " + mFrames + ", st " + mSurfaceTexture);
        }
        synchronized (mLock) {
            mLock.notify();
            if (mSurfaceTexture == null) {
                lockWait(mLock);
                return;
            }
            try {
                mSurfaceTexture.updateTexImage();
            } catch (Exception e) {
                lockWait(mLock);
                return;
            }
            mSurfaceTexture.getTransformMatrix(mTransformMatrix);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
            mTextureProgram.drawFrame(mTexId, mTransformMatrix, GlUtil.IDENTITY_MATRIX);
            // 保存第 ? 帧图像
            if (mFrames % 100 == 0) {
                mByteBuffer.rewind();
                GLES20.glReadPixels(0, 0, mWidth, mHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, mByteBuffer);
                String filePath = new File(mDestDir, mFrames + ".png").getAbsolutePath();
                saveFrame(filePath);
            }
            lockWait(mLock);
        }
    }

    private void lockWait(Object lock) {
        try {
            lock.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void saveFrame(String filePath) {
        try (OutputStream fos = new FileOutputStream(filePath)) {
            Matrix matrix = new Matrix();
            matrix.preScale(1f, -1f);
            Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
            mByteBuffer.rewind();
            bmp.copyPixelsFromBuffer(mByteBuffer);
            Bitmap scaled = Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), matrix, false);
            bmp.recycle();
            scaled.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (IOException e) {
            Log.e(TAG, "saveFrame: ", e);
        }
        Log.d(TAG, "Saved " + mWidth + "x" + mHeight + " frame as " + filePath);
    }

}
