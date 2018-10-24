package com.richie.multimedialearning.audiorecord;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;

/**
 * Audio record
 */
public class AudioRecordActivity extends AppCompatActivity implements View.OnClickListener {
    private final ILogger logger = LoggerFactory.getLogger(AudioRecordActivity.class);
    private AudioRecorder mAudioRecorder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        findViewById(R.id.btn_start_record_audio).setOnClickListener(this);
        findViewById(R.id.btn_stop_record_audio).setOnClickListener(this);
        findViewById(R.id.btn_pause_record_audio).setOnClickListener(this);

        mAudioRecorder = new AudioRecorder(this);
        mAudioRecorder.createDefaultAudio("hello");
        mAudioRecorder.setRecordStreamListener(new AudioRecorder.RecordStreamListener() {

            @Override
            public void onRecording(byte[] bytes, int offset, int length) {
                //logger.debug("onRecording. bytes:{}, offset:{}, length:{}", Arrays.toString(bytes), offset, length);
            }

            @Override
            public void finishRecord() {
                logger.debug("finishRecord");
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_record_audio:
                mAudioRecorder.startRecord();
                break;
            case R.id.btn_stop_record_audio:
                mAudioRecorder.stopRecord();
                break;
            case R.id.btn_pause_record_audio:
                mAudioRecorder.pauseRecord();
                break;
            default:
        }
    }
}
