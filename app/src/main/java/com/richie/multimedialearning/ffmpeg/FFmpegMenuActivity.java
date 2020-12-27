package com.richie.multimedialearning.ffmpeg;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.richie.multimedialearning.R;

import cn.richie.ffmpeg.FFmpegNative;

public class FFmpegMenuActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "FFmpegMenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ffmpeg_menu);

        findViewById(R.id.btn_ffmpeg_video).setOnClickListener(this);
        findViewById(R.id.btn_ffmpeg_audio).setOnClickListener(this);

        String version = FFmpegNative.getVersion();
        Log.d(TAG, "onCreate: ffmpeg version: " + version);
        Toast.makeText(this, version, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_ffmpeg_video) {
            Intent intent = new Intent(this, FFmpegVideoActivity.class);
            startActivity(intent);
        } else if (id == R.id.btn_ffmpeg_audio) {

        }
    }
}