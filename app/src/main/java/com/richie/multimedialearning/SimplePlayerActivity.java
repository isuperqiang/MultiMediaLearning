package com.richie.multimedialearning;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.multimedialearning.media.BaseDecoder;
import com.richie.multimedialearning.media.DecoderStateListenerAdapter;
import com.richie.multimedialearning.media.decoder.AudioDecoder;
import com.richie.multimedialearning.media.decoder.VideoDecoder;
import com.richie.multimedialearning.utils.FileUtils;

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

        Log.d(TAG, "onCreate: ");
    }

    private void initPlayer() {
        File dir = FileUtils.getExternalAssetsDir(this);
        File videoFile = new File(dir, "sample.mp4");
        String filePath = videoFile.getAbsolutePath();
        mAudioDecoder = new AudioDecoder(filePath);
        mVideoDecoder = new VideoDecoder(filePath);

        mAudioDecoder.setStateListener(new DecoderStateListenerAdapter() {
            @Override
            public void decoderPrepare(BaseDecoder decoder) {
                super.decoderPrepare(decoder);
//                mAudioDecoder.goOn();
                Log.i(TAG, "decoderPrepare: audio");
            }
        });

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
                mVideoDecoder.setSurface(holder.getSurface());
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

//        mAudioDecoder.goOn();
//        mVideoDecoder.goOn();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioDecoder.stop();
        mVideoDecoder.stop();
    }
}