package com.richie.multimedialearning.codec;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.audiotrack.AudioTracker;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.File;
import java.io.IOException;


public class CodecActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(CodecActivity.class);
    private AudioEncoder mAudioEncoder;
    private File mFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codec);
        ViewClickListener viewClickListener = new ViewClickListener();
        findViewById(R.id.btn_start_encode_audio).setOnClickListener(viewClickListener);
        findViewById(R.id.btn_stop_encode_audio).setOnClickListener(viewClickListener);
        findViewById(R.id.btn_start_decode_audio).setOnClickListener(viewClickListener);
    }

    private class ViewClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_encode_audio: {
                    ThreadHelper.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            mFile = new File(FileUtils.getFileDir(CodecActivity.this), FileUtils.getUUID32() + ".aac");
                            logger.info("out file:{}", mFile.getAbsolutePath());
                            mAudioEncoder = new AudioEncoder();
                            mAudioEncoder.createAudio();
                            try {
                                mAudioEncoder.createMediaCodec();
                                mAudioEncoder.start(mFile);
                            } catch (Exception e) {
                                logger.error(e);
                            }
                        }
                    });
                }
                break;
                case R.id.btn_stop_encode_audio: {
                    if (mAudioEncoder != null) {
                        mAudioEncoder.stop();
                    }
                }
                break;
                case R.id.btn_start_decode_audio: {
                    logger.debug("onClick() called with: file = [" + mFile + "]");
                    if (mFile != null) {
                        File pcmFile = new File(FileUtils.getFileDir(CodecActivity.this), mFile.getName() + ".pcm");
                        try {
                            new AudioDecoder().decodeAac2Pcm(mFile, pcmFile);
                            AudioTracker audioTracker = new AudioTracker(CodecActivity.this);
                            audioTracker.createAudioTrack(pcmFile.getAbsolutePath());
                            audioTracker.start();
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                }
                break;
                default:
            }
        }
    }

}
