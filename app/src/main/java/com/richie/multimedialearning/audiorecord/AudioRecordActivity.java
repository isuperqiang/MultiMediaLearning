package com.richie.multimedialearning.audiorecord;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.Button;
import android.widget.Toast;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

/**
 * 使用 AudioRecord 录音
 *
 * @author Richie on 2018.10.15
 */
public class AudioRecordActivity extends AppCompatActivity implements View.OnTouchListener {
    private final ILogger logger = LoggerFactory.getLogger(AudioRecordActivity.class);
    private AudioRecorder mAudioRecorder;
    private Button mBtnRecord;
    private int mLongPressTimeout = ViewConfiguration.getLongPressTimeout();
    private Handler mMainHandler = new Handler();
    private boolean mCanceled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_record);
        mBtnRecord = findViewById(R.id.btn_record_audio);
        mBtnRecord.setOnTouchListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAudioRecorder != null) {
            mAudioRecorder.release();
            mAudioRecorder = null;
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN: {
                mCanceled = false;
                mBtnRecord.setText("松开结束");
                mMainHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!mCanceled) {
                            startRecord();
                        }
                    }
                }, mLongPressTimeout);
            }
            break;
            case MotionEvent.ACTION_UP: {
                mCanceled = true;
                mBtnRecord.setText("长按录音");
                mMainHandler.removeCallbacksAndMessages(null);
                if (mAudioRecorder != null) {
                    mAudioRecorder.stopRecord();
                }
            }
            break;
            default:
        }
        return true;
    }

    private void startRecord() {
        mAudioRecorder = new AudioRecorder(this);
        mAudioRecorder.createDefaultAudio(FileUtils.getUUID32());
        mAudioRecorder.setRecordStreamListener(new AudioRecorder.RecordStreamListener() {
            @Override
            public void onStart() {
                logger.debug("onStart");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this, "录音开始", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onRecord(byte[] bytes, int length) {
                logger.verbose("onRecord. byte length:{}", length);
            }

            @Override
            public void onStop() {
                logger.debug("onStop");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this, "录音结束", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onError(String message) {
                logger.warn("onError: {}", message);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(AudioRecordActivity.this, "录音错误 " + message, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        mAudioRecorder.startRecord();
    }

}
