package com.richie.multimedialearning;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.richie.multimedialearning.audiorecord.AudioRecordActivity;
import com.richie.multimedialearning.audiotrack.AudioTrackActivity;
import com.richie.multimedialearning.surfaceview.SurfaceActivity;

/**
 * Main
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.btn_surface_view).setOnClickListener(this);
        findViewById(R.id.btn_audio_record).setOnClickListener(this);
        findViewById(R.id.btn_audio_track).setOnClickListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 0);
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent = null;
        switch (v.getId()) {
            case R.id.btn_surface_view:
                intent = new Intent(this, SurfaceActivity.class);
                break;
            case R.id.btn_audio_record:
                intent = new Intent(this, AudioRecordActivity.class);
                break;
            case R.id.btn_audio_track:
                intent = new Intent(this, AudioTrackActivity.class);
                break;
            default:
        }
        if (intent != null) {
            startActivity(intent);
        }
    }
}
