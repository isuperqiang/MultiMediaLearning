package com.richie.multimedialearning.audiotrack;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;

/**
 * 使用 AudioTrack 播放音频
 *
 * @author Richie on 2018.10.26
 */
public class AudioTrackActivity extends AppCompatActivity implements View.OnClickListener {
    private final ILogger logger = LoggerFactory.getLogger(AudioTrackActivity.class);
    private AudioTracker mAudioTracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_track);
        findViewById(R.id.btn_start_play_audio).setOnClickListener(this);
        findViewById(R.id.btn_stop_play_audio).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_play_audio: {
                mAudioTracker = new AudioTracker();
                File pcmFile = new File(FileUtils.getExternalAssetsDir(this), "test.pcm");
                mAudioTracker.createAudioTrack(pcmFile.getAbsolutePath());
                mAudioTracker.setAudioPlayListener(new AudioTracker.AudioPlayListener() {
                    @Override
                    public void onStart() {
                        logger.debug("onStart");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AudioTrackActivity.this, "播放开始", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onStop() {
                        logger.debug("onStop");
                        mAudioTracker.release();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AudioTrackActivity.this, "播放结束", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        logger.warn("onError: {}", message);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(AudioTrackActivity.this, "播放错误 " + message, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
                mAudioTracker.start();
            }
            break;
            case R.id.btn_stop_play_audio: {
                stopAudio();
            }
            break;
            default:
        }
    }

    private void stopAudio() {
        if (mAudioTracker != null) {
            try {
                mAudioTracker.stop();
            } catch (IllegalStateException e) {
                logger.warn(e);
            }
        }
    }

}
