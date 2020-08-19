package com.richie.multimedialearning.mediacodec;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.File;
import java.io.IOException;

/**
 * blog:
 * - https://yedaxia.github.io/Android-MediaExtractor-And-MediaCodec/
 */
public class CodecActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(CodecActivity.class);
    private AudioEncoder mAudioEncoder;
    private Button mBtnStartRecord;
    private boolean mIsRecording;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codec);
        ViewClickListener viewClickListener = new ViewClickListener();
        mBtnStartRecord = findViewById(R.id.btn_start_record_encode_audio);
        mBtnStartRecord.setOnClickListener(viewClickListener);
        findViewById(R.id.btn_start_decode_audio).setOnClickListener(viewClickListener);
        findViewById(R.id.btn_start_encode_audio).setOnClickListener(viewClickListener);
        findViewById(R.id.btn_start_camera).setOnClickListener(viewClickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecord();
    }

    private void stopRecord() {
        if (mAudioEncoder != null) {
            mAudioEncoder.stop();
            mAudioEncoder = null;
        }
    }

    private class ViewClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_record_encode_audio: {
                    if (mIsRecording) {
                        mBtnStartRecord.setText("开始录音并编码音频");
                        mIsRecording = false;
                        stopRecord();
                    } else {
                        mBtnStartRecord.setText("停止录音和编码音频");
                        mIsRecording = true;
                        File file = new File(FileUtils.getAacFileDir(CodecActivity.this), FileUtils.getUUID32() + ".aac");
                        logger.info("out file:{}", file.getAbsolutePath());
                        mAudioEncoder = new AudioEncoder();
                        mAudioEncoder.createAudio();
                        try {
                            mAudioEncoder.createMediaCodec();
                            mAudioEncoder.start(file);
                        } catch (IOException e) {
                            logger.error(e);
                        }
                    }
                }
                break;
                case R.id.btn_start_encode_audio: {
                    ThreadHelper.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            // 44.1kHz采样率，单通道，16位深
                            File pcmFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "test.pcm");
                            File aacFile = new File(FileUtils.getAacFileDir(CodecActivity.this), "test_output.aac");
                            if (aacFile.exists()) {
                                aacFile.delete();
                            }
                            try {
                                logger.info("startEncode");
                                AacPcmCoder.encodePcmToAac(pcmFile, aacFile);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CodecActivity.this, "编码完成", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    });
                }
                break;
                case R.id.btn_start_decode_audio: {
                    ThreadHelper.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            // 44.1kHz采样率，单通道
                            File aacFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "test.aac");
                            File pcmFile = new File(FileUtils.getPcmFileDir(CodecActivity.this), "test_output.pcm");
                            try {
                                AacPcmCoder.decodeAacToPcm(aacFile, pcmFile);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CodecActivity.this, "解码完成", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    });
                }
                break;
                case R.id.btn_start_camera: {
                    Intent intent = new Intent(CodecActivity.this, CameraOpenGlActivity.class);
                    startActivity(intent);
                }
                break;
                default:
            }
        }
    }

}
