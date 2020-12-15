package com.richie.multimedialearning.mediacodec;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.File;
import java.io.IOException;

/**
 * MediaCodec 使用案例
 * - https://yedaxia.github.io/Android-MediaExtractor-And-MediaCodec/
 *
 * @author Richie on 2018.10.22
 */
public class CodecActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(CodecActivity.class);
    private AudioRecordEncoder mAudioRecordEncoder;
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
        findViewById(R.id.btn_start_decode_video).setOnClickListener(viewClickListener);
        findViewById(R.id.btn_start_encode_video).setOnClickListener(viewClickListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRecord();
    }

    private void stopRecord() {
        if (mAudioRecordEncoder != null) {
            mAudioRecordEncoder.stop();
            mAudioRecordEncoder = null;
        }
    }

    private class ViewClickListener implements View.OnClickListener {

        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.btn_start_record_encode_audio: {
                    if (mIsRecording) {
                        mBtnStartRecord.setText("开始[录音并编码音频]");
                        mIsRecording = false;
                        stopRecord();
                    } else {
                        mBtnStartRecord.setText("停止[录音和编码音频]");
                        mIsRecording = true;
                        File file = new File(FileUtils.getAacFileDir(CodecActivity.this), FileUtils.getUUID32() + ".aac");
                        logger.info("out file:{}", file.getAbsolutePath());
                        mAudioRecordEncoder = new AudioRecordEncoder();
                        mAudioRecordEncoder.createAudio();
                        try {
                            mAudioRecordEncoder.createMediaCodec();
                            mAudioRecordEncoder.start(file);
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
                            File pcmFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "sample.pcm");
                            File aacFile = new File(FileUtils.getAacFileDir(CodecActivity.this), "aac_output.aac");
                            if (aacFile.exists()) {
                                aacFile.delete();
                            }
                            try {
                                AacPcmCodec.encodePcmToAac(pcmFile, aacFile);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CodecActivity.this, "音频编码完成", Toast.LENGTH_SHORT).show();
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
                            File aacFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "sample.aac");
                            File pcmFile = new File(FileUtils.getPcmFileDir(CodecActivity.this), "pcm_output.pcm");
                            try {
                                AacPcmCodec.decodeAacToPcm(aacFile, pcmFile);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CodecActivity.this, "音频解码完成", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(e);
                            }
                        }
                    });
                }
                break;
                case R.id.btn_start_decode_video: {
                    // 解码 mp4 视频，并渲染到 Surface 上，读取 RGBA 数据
                    v.setEnabled(false);
                    ThreadHelper.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            File src = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "sample.mp4");
                            File dest = FileUtils.getRgbaFileDir(CodecActivity.this);
                            try {
                                new AvcRgbaCodec().decode(src, dest);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(CodecActivity.this, "视频解码完成", Toast.LENGTH_SHORT).show();
                                        v.setEnabled(true);
                                    }
                                });
                            } catch (IOException e) {
                                logger.error(e);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        v.setEnabled(true);
                                    }
                                });
                            }
                        }
                    });
                }
                break;
                case R.id.btn_start_encode_video: {
                    Intent intent = new Intent(CodecActivity.this, CameraOpenGlActivity.class);
                    startActivity(intent);
                }
                break;
                default:
            }
        }
    }

}
