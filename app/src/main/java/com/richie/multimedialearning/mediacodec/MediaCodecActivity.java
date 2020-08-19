package com.richie.multimedialearning.mediacodec;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.BarUtils;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * MediaCodec
 */
public class MediaCodecActivity extends AppCompatActivity {
    private static final String VIDEO_SOURCE_PATH = "input.mp4";
    private final ILogger logger = LoggerFactory.getLogger(MediaCodecActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        BarUtils.setStatusBarVisibility(this, false);
        ConstraintLayout constraintLayout = findViewById(R.id.cl_root);
        //CameraSurfaceView cameraSurfaceView = new CameraSurfaceView(this);
        //constraintLayout.addView(cameraSurfaceView, 0);

        SurfaceView surfaceView = findViewById(R.id.surface_view);
        surfaceView.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        surfaceView.getHolder().addCallback(new MySurfaceCallback());

        logger.info("supportH264Codec:{}", supportH264Codec());
    }

    private boolean supportH264Codec() {
        // 遍历支持的编码格式信息
        for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if ("video/avc".equalsIgnoreCase(type)) {
                    return true;
                }
            }
        }
        return false;
    }

    private class MySurfaceCallback implements SurfaceHolder.Callback, Runnable {
        private MediaCodec mMediaCodec;
        private Surface mSurface;
        private volatile boolean mIsDestroyed;

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            logger.debug("surfaceCreated() called with: holder = [" + holder + "]");
            mSurface = holder.getSurface();
            mIsDestroyed = false;
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            logger.debug("surfaceChanged() called with: holder = [" + holder + "], format = [" + format + "], width = [" + width + "], height = [" + height + "]");
            Thread thread = new Thread(this);
            thread.start();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            logger.debug("surfaceDestroyed() called with: holder = [" + holder + "]");
            mIsDestroyed = true;
        }

        @Override
        public void run() {
            MediaExtractor mediaExtractor = new MediaExtractor();
            try {
                mediaExtractor.setDataSource(new File(FileUtils.getFileDir(MediaCodecActivity.this), VIDEO_SOURCE_PATH).getAbsolutePath());
            } catch (IOException e) {
                logger.error(e);
            }

            for (int i = 0, j = mediaExtractor.getTrackCount(); i < j; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    mediaExtractor.selectTrack(i);
                    int width = trackFormat.getInteger(MediaFormat.KEY_WIDTH);
                    int height = trackFormat.getInteger(MediaFormat.KEY_HEIGHT);
                    //int rotation = trackFormat.getInteger(MediaFormat.KEY_ROTATION);
                    logger.info("mimeType:{}, sampleTime:{}, width:{}, height:{}",
                            mimeType, mediaExtractor.getSampleTime(), width, height);
                    try {
                        mMediaCodec = MediaCodec.createDecoderByType(mimeType);
                        mMediaCodec.configure(trackFormat, mSurface, null, 0);
                    } catch (IOException e) {
                        logger.error(e);
                    }
                    break;
                }
            }

            if (mMediaCodec == null) {
                return;
            }

            logger.info("codec start");
            mMediaCodec.start();
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
            ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
            boolean isEos = false;
            long startMs = System.currentTimeMillis();
            while (!mIsDestroyed) {
                if (!isEos) {
                    int inputBufferId = mMediaCodec.dequeueInputBuffer(10000);
                    if (inputBufferId >= 0) {
                        ByteBuffer inputBuffer = inputBuffers[inputBufferId];
                        int sampleSize = mediaExtractor.readSampleData(inputBuffer, 0);
                        if (sampleSize < 0) {
                            logger.info("end of stream");
                            mMediaCodec.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEos = true;
                        } else {
                            mMediaCodec.queueInputBuffer(inputBufferId, 0, sampleSize, mediaExtractor.getSampleTime(), 0);
                            mediaExtractor.advance();
                        }
                    }
                }

                int outputBufferId = mMediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                switch (outputBufferId) {
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        //outputBuffers = mMediaCodec.getOutputBuffers();
                        break;
                    default:
                        //ByteBuffer outputBuffer = outputBuffers[outputBufferId];
                        while (bufferInfo.presentationTimeUs / 1000 > System.currentTimeMillis() - startMs) {
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                logger.error(e);
                                break;
                            }
                        }
                        mMediaCodec.releaseOutputBuffer(outputBufferId, true);
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    logger.info("BUFFER_FLAG_END_OF_STREAM");
                    break;
                }
            }

            mMediaCodec.stop();
            mMediaCodec.release();
            mediaExtractor.release();
        }
    }

}
