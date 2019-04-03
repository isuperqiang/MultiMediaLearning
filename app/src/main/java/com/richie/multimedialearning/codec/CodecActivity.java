package com.richie.multimedialearning.codec;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.audiorecord.PcmToWav;
import com.richie.multimedialearning.utils.FileUtils;
import com.richie.multimedialearning.utils.ThreadHelper;

import java.io.File;

/**
 * blog:
 * - https://yedaxia.github.io/Android-MediaExtractor-And-MediaCodec/
 */

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
                    File pcmFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "test.pcm");
                    File wavFile = new File(FileUtils.getWavFileDir(CodecActivity.this), "test.wav");
                    PcmToWav.makePCMFileToWAVFile(pcmFile.getAbsolutePath(), wavFile.getAbsolutePath(), false);
                }
                break;
                case R.id.btn_start_decode_audio: {
                    logger.debug("onClick() called with: file = [" + mFile + "]");
                    //if (mFile != null) {
                    //    File pcmFile = new File(FileUtils.getFileDir(CodecActivity.this), mFile.getName() + ".pcm");
                    //    try {
                    //        AacAudioDecoder.decodeAac2Pcm(mFile, pcmFile);
                    //        File aacFile = new File(FileUtils.getFileDir(CodecActivity.this), pcmFile.getName() + ".aac");
                    //        AACAudioEncoder.encodeToFile(pcmFile, aacFile);
                    //        //AudioTracker audioTracker = new AudioTracker(CodecActivity.this);
                    //        //audioTracker.createAudioTrack(pcmFile.getAbsolutePath());
                    //        //audioTracker.start();
                    //    } catch (IOException e) {
                    //        logger.error(e);
                    //    }
                    // }
                    ThreadHelper.getInstance().execute(new Runnable() {
                        @Override
                        public void run() {
                            File aacFile = new File(FileUtils.getWavFileDir(CodecActivity.this), "test.aac");
                            File pcmFile = new File(FileUtils.getExternalAssetsDir(CodecActivity.this), "test.pcm");
                            //AudioTracker audioTracker = new AudioTracker(CodecActivity.this);
                            //audioTracker.createAudioTrack(pcmFile.getAbsolutePath());
                            //audioTracker.start();
                            AacAudioEncoder.encodeToFile(pcmFile, aacFile);
                        }
                    });
                }
                break;
                default:
            }
        }
    }

}
