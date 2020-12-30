package com.richie.multimedialearning.utils;

import android.media.MediaMetadataRetriever;
import android.util.Log;

/**
 * @author Richie on 2020.12.30
 */
public final class MediaUtils {
    private static final String TAG = "MediaUtil";

    public static MediaMetaData retrieveMediaInfo(String path) {
        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
        MediaMetaData mediaMetaData = new MediaMetaData();
        try {
            mediaMetadataRetriever.setDataSource(path);
            int videoWidth = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int videoHeight = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            int videoRotation = Integer.parseInt(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));
            mediaMetaData.width = videoWidth;
            mediaMetaData.height = videoHeight;
            mediaMetaData.rotation = videoRotation;
            return mediaMetaData;
        } catch (Exception e) {
            Log.w(TAG, "retrieveMediaInfo: ", e);
        } finally {
            mediaMetadataRetriever.release();
        }
        return mediaMetaData;
    }

    public static final class MediaMetaData {
        public int width;
        public int height;
        public int rotation;
    }
}
