package com.richie.multimedialearning.player;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.URLUtil;

import androidx.annotation.Nullable;

import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.mediacodec.MediaCodecInfo;
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author LiuQiang on 2019.01.28
 * 封装 ExoPlayer，替代 MediaPlayer API
 */
public class ExoMediaPlayer extends BaseMediaPlayer {
    private static final String TAG = "ExoMediaPlayer";
    private SimpleExoPlayer mSimpleExoPlayer;
    private Context mContext;
    private String mDataSource;

    public ExoMediaPlayer(Context context) {
        mContext = context.getApplicationContext();
        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context);
        if (isMTKDevice()) {
            renderersFactory.setMediaCodecSelector(new MediaCodecSelector() {
                @Override
                public List<MediaCodecInfo> getDecoderInfos(String mimeType, boolean requiresSecureDecoder) throws MediaCodecUtil.DecoderQueryException {
                    List<MediaCodecInfo> decoderInfos;
                    if (mimeType.equals(MimeTypes.AUDIO_RAW)) {
                        decoderInfos = new ArrayList<>(1);
                        MediaCodecInfo mediaCodecInfo = MediaCodecInfo.newInstance("OMX.google.raw.decoder", MimeTypes.AUDIO_RAW, null);
                        decoderInfos.add(mediaCodecInfo);
                    } else {
                        decoderInfos = MediaCodecUtil.getDecoderInfos(mimeType, requiresSecureDecoder);
                    }
                    return decoderInfos.isEmpty()
                            ? Collections.emptyList()
                            : Collections.singletonList(decoderInfos.get(0));
                }

                @Nullable
                @Override
                public MediaCodecInfo getPassthroughDecoderInfo() throws MediaCodecUtil.DecoderQueryException {
                    return MediaCodecUtil.getPassthroughDecoderInfo();
                }
            });
        }
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, renderersFactory,
                new DefaultTrackSelector());
        mSimpleExoPlayer.addListener(new MediaEventListener());
    }

    // MediaCodecUtil#isCodecUsableDecoder 不包含下面的处理，所以在这里加上
    private boolean isMTKDevice() {
        try {
            MediaCodecInfo decoderInfo = MediaCodecUtil.getDecoderInfo(MimeTypes.AUDIO_RAW, false);
            Log.i(TAG, "decoderInfo " + decoderInfo);
            if (decoderInfo != null) {
                return "OMX.MTK.AUDIO.DECODER.RAW".equals(decoderInfo.name);
            }
        } catch (MediaCodecUtil.DecoderQueryException e) {
            Log.e(TAG, "isMTKDevice: ", e);
        }
        return false;
    }

    @Override
    public void start() {
        boolean ready = mSimpleExoPlayer.getPlaybackState() == Player.STATE_READY;
        if (ready && !mSimpleExoPlayer.getPlayWhenReady()) {
            mSimpleExoPlayer.setPlayWhenReady(true);
        } else {
            setDataSource(mDataSource);
        }
    }

    @Override
    public void pause() {
        if (isPlaying()) {
            mSimpleExoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    public void stop() {
        mSimpleExoPlayer.stop();
    }

    @Override
    public void release() {
        mSimpleExoPlayer.release();
        clearListener();
    }

    @Override
    public void seekTo(long position) {
        mSimpleExoPlayer.seekTo(position);
    }

    @Override
    public long getDuration() {
        return mSimpleExoPlayer.getDuration();
    }

    @Override
    public long getCurrentPosition() {
        return mSimpleExoPlayer.getCurrentPosition();
    }

    @Override
    public void setDataSource(String pathOrUrl) {
        if (TextUtils.isEmpty(pathOrUrl)) {
            return;
        }

        mDataSource = pathOrUrl;
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(mContext,
                Util.getUserAgent(mContext, mContext.getPackageName()));
        ExtractorMediaSource.Factory mediaSourceFactory = new ExtractorMediaSource.Factory(dataSourceFactory);
        Uri uri;
        if (URLUtil.isValidUrl(pathOrUrl)) {
            uri = Uri.parse(pathOrUrl);
        } else {
            uri = Uri.fromFile(new File(pathOrUrl));
        }
        MediaSource mediaSource = mediaSourceFactory.createMediaSource(uri);
        mSimpleExoPlayer.setPlayWhenReady(true);
        mSimpleExoPlayer.prepare(mediaSource);
    }

    @Override
    public boolean isPlaying() {
        boolean ready = mSimpleExoPlayer.getPlaybackState() == Player.STATE_READY;
        return ready && mSimpleExoPlayer.getPlayWhenReady();
    }

    @Override
    public void setLooping(boolean isLooping) {
        mSimpleExoPlayer.setRepeatMode(isLooping ? Player.REPEAT_MODE_ALL : Player.REPEAT_MODE_OFF);
    }

    @Override
    public void setAudioStreamType(int type) {
        mSimpleExoPlayer.setAudioStreamType(type);
    }

    @Override
    public void reset() {
        mSimpleExoPlayer.stop(true);
    }

    @Override
    public void prepareAsync() {
    }

    @Override
    public void setVolume(float audioVolume) {
        mSimpleExoPlayer.setVolume(audioVolume);
    }

    @Override
    public int getAudioSessionId() {
        return mSimpleExoPlayer.getAudioSessionId();
    }

    @Override
    public void setAudioSessionId(int sessionId) {
    }

    private class MediaEventListener implements Player.EventListener {

        @Override
        public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
            switch (playbackState) {
//                case Player.STATE_IDLE:
//                    break;
//                case Player.STATE_BUFFERING:
//                    break;
                case Player.STATE_READY:
                    if (playWhenReady) {
                        notifyOnPrepared();
                    }
                    break;
                case Player.STATE_ENDED:
                    notifyOnCompletion();
                    break;
                default:
            }
        }

        @Override
        public void onPlayerError(ExoPlaybackException error) {
            Log.w(TAG, "onPlayerError: ", error);
            String message;
            switch (error.type) {
                case ExoPlaybackException.TYPE_SOURCE:
                    message = "数据源异常";
                    break;
                case ExoPlaybackException.TYPE_RENDERER:
                    message = "解码异常";
                    break;
                case ExoPlaybackException.TYPE_UNEXPECTED:
                default:
                    message = "其他异常";
            }
            notifyOnError(error.type, message);
        }
    }
}
