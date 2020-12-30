package com.richie.multimedialearning.media.decoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.multimedialearning.media.BaseDecoder;
import com.richie.multimedialearning.media.IExtractor;
import com.richie.multimedialearning.media.extractor.VideoExtractor;

import java.nio.ByteBuffer;

/**
 * @author Richie on 2020.12.30
 */
public class VideoDecoder extends BaseDecoder {
    private static final String TAG = "VideoDecoder";
    private Surface mSurface;
    private SurfaceView mSurfaceView;

    public VideoDecoder(String filePath) {
        super(filePath);
    }

    public void setSurfaceView(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    @Override
    protected boolean check() {
        if (mSurface == null && mSurfaceView == null) {
            Log.w(TAG, "Surface 和 SurfaceView 都为 null");
            if (mStateListener != null) {
                mStateListener.decoderError(this, "显示器为空");
            }
            return false;
        }
        return true;
    }

    @Override
    protected IExtractor initExtractor(String filePath) {
        return new VideoExtractor(filePath);
    }

    @Override
    protected void initSpecParams(MediaFormat mediaFormat) {

    }

    @Override
    protected boolean configCodec(final MediaCodec mediaCodec, final MediaFormat mediaFormat) {
        Log.d(TAG, "configCodec: ");
        if (mSurface != null) {
            mediaCodec.configure(mediaFormat, mSurface, null, 0);
            notifyDecode();
//        } else if (mSurfaceView.getHolder().getSurface() != null) {
//            mSurface = mSurfaceView.getHolder().getSurface();
//            configCodec(mediaCodec, mediaFormat);
        } else {
            mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    Log.i(TAG, "surfaceCreated: ");
                    mSurface = holder.getSurface();
                    configCodec(mediaCodec, mediaFormat);
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {

                }
            });
            return false;
        }
        return true;
    }

    @Override
    protected boolean initRender() {
        return true;
    }

    @Override
    protected void render(ByteBuffer outputBuffer, MediaCodec.BufferInfo bufferInfo) {

    }

    @Override
    protected void doneDecode() {

    }
}
