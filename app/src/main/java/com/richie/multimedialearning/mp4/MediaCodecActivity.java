package com.richie.multimedialearning.mp4;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.BarUtils;

/**
 * MediaCodec
 */
public class MediaCodecActivity extends AppCompatActivity {
    private final ILogger logger = LoggerFactory.getLogger(MediaCodecActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_codec);
        BarUtils.setStatusBarVisibility(this, false);
        ConstraintLayout constraintLayout = findViewById(R.id.cl_root);
        CameraSurfaceView cameraSurfaceView = new CameraSurfaceView(this);
        constraintLayout.addView(cameraSurfaceView, 0);

        logger.debug("supportH264Codec:{}", supportH264Codec());
    }

    private boolean supportH264Codec() {
        // 遍历支持的编码格式信息
        for (int j = MediaCodecList.getCodecCount() - 1; j >= 0; j--) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(j);
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if ("video/avc".equalsIgnoreCase(type)) {
                    return true;
                }
            }
        }
        return false;
    }

}
