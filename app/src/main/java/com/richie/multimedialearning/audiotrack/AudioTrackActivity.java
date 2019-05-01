package com.richie.multimedialearning.audiotrack;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;

/**
 * 使用 AudioTrack 播放音频
 */
public class AudioTrackActivity extends AppCompatActivity implements View.OnClickListener {
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
        mAudioTracker.release();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_play_audio: {
                mAudioTracker = new AudioTracker(this);
                File pcmFile = new File(FileUtils.getExternalAssetsDir(this), "test.pcm");
                mAudioTracker.createAudioTrack(pcmFile.getAbsolutePath());
                mAudioTracker.start();
            }
            break;
            case R.id.btn_stop_play_audio: {
                try {
                    mAudioTracker.stop();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            default:
        }
    }
}
