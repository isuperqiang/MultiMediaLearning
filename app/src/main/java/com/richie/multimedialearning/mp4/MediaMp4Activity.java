package com.richie.multimedialearning.mp4;

import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;
import java.io.IOException;

public class MediaMp4Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_mp4);
    }

    private void r() {
        String path = new File(FileUtils.getFileDir(this), "a.mp4").getAbsolutePath();
        try {
            MediaMuxer mediaMuxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            mediaMuxer.addTrack(new MediaFormat());
            MediaFormat.createAudioFormat("audio/raw", 16000, 2);
            MediaFormat.createVideoFormat("video/avc", 1280, 720);
            mediaMuxer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
