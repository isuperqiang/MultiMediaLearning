package com.richie.multimedialearning;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceView;

/**
 * SurfaceView
 */
public class SurfaceActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_surface);
        SurfaceView surfaceView = findViewById(R.id.surface_view);
    }
}
