package com.richie.multimedialearning;

import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.richie.multimedialearning.media.decoder.AudioDecoder;
import com.richie.multimedialearning.media.decoder.BaseDecoder;
import com.richie.multimedialearning.media.decoder.DecoderStateListenerAdapter;
import com.richie.multimedialearning.media.decoder.VideoDecoder;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.MediaUtils;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimplePlayerActivity extends AppCompatActivity {
    private static final String TAG = "SimplePlayerActivity";
    private AudioDecoder mAudioDecoder;
    private VideoDecoder mVideoDecoder;
    private SurfaceView mSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_player);
        mSurfaceView = findViewById(R.id.surface_view);
        initPlayer();
    }

    private void initPlayer() {
        File videoFile = new File(FileUtils.getExternalAssetsDir(this), "sample.mp4");
        String filePath = videoFile.getAbsolutePath();
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        MediaUtils.MediaMetaData mediaMetaData = MediaUtils.retrieveMediaInfo(filePath);
        float scale = ((float) point.x / point.y) / ((float) mediaMetaData.width / mediaMetaData.height);
        int width;
        int height;
        if (scale < 1) {
            width = point.x;
            scale = (float) point.x / mediaMetaData.width;
            height = (int) (mediaMetaData.height * scale);
        } else if (scale > 1) {
            height = point.y;
            scale = (float) point.y / mediaMetaData.height;
            width = (int) (mediaMetaData.width * scale);
        } else {
            width = point.x;
            height = point.y;
        }
        ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) mSurfaceView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mSurfaceView.setLayoutParams(layoutParams);
        Log.i(TAG, "initPlayer: video width " + mediaMetaData.width + ", height " + mediaMetaData.height + ", rotation " + mediaMetaData.rotation);

        mAudioDecoder = new AudioDecoder(filePath);
        mAudioDecoder.setStateListener(new DecoderStateListenerAdapter() {
            @Override
            public void decoderPrepare(BaseDecoder decoder) {
                super.decoderPrepare(decoder);
//                mAudioDecoder.goOn();
                Log.i(TAG, "decoderPrepare: audio");
            }
        });

        mVideoDecoder = new VideoDecoder(filePath, mSurfaceView);
        mVideoDecoder.setStateListener(new DecoderStateListenerAdapter() {
            @Override
            public void decoderPrepare(BaseDecoder decoder) {
                super.decoderPrepare(decoder);
                Log.i(TAG, "decoderPrepare: video");
            }
        });

        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated: ");
                mVideoDecoder = new VideoDecoder(filePath, holder.getSurface());
                executorService.execute(mAudioDecoder);
                executorService.execute(mVideoDecoder);
                mAudioDecoder.goOn();
                mVideoDecoder.goOn();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioDecoder != null) {
            mAudioDecoder.stop();
        }
        if (mVideoDecoder != null) {
            mVideoDecoder.stop();
        }
    }
}