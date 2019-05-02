package com.richie.multimedialearning.utils;

import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.media.AudioManager;

/**
 * @author Richie on 2019.03.23
 * 声音管理
 */
public final class AudioObserver implements LifecycleObserver {

    private Context mContext;

    public AudioObserver(Context context) {
        mContext = context;
    }

    @OnLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_START)
    public void onStart() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    @OnLifecycleEvent(android.arch.lifecycle.Lifecycle.Event.ON_STOP)
    public void oStop() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.abandonAudioFocus(null);
    }


}
