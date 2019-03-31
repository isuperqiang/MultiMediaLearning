package com.richie.multimedialearning;

import android.app.Application;
import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Richie on 2018.10.17
 */
public class MultiMediaApp extends Application {
    private static Context sContext;

    public static Context getContext() {
        return sContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sContext = this;
        initAssets();
    }

    private void initAssets() {
        File file = new File(getExternalFilesDir(null), "template.jpg");
        if (file.exists()) {
            return;
        }

        try {
            InputStream is = getAssets().open("template.jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            fos.write(bytes);
            fos.flush();
            fos.close();
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
