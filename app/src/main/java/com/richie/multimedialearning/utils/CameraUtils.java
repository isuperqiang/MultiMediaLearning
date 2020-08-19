package com.richie.multimedialearning.utils;

import android.app.Activity;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import java.util.List;

/**
 * @author Richie on 2018.10.27
 */
public final class CameraUtils {
    private static final String TAG = "CameraUtils";
    private static boolean DEBUG = false;
    /**
     * 相机宽高
     */
    public static final int PREVIEW_WIDTH = 1920;
    public static final int PREVIEW_HEIGHT = 1080;

    private CameraUtils() {
    }

    public static int getCameraOrientation(int cameraId) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        return info.orientation;
    }

    public static void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
            default:
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    public static List<Integer> getSupportedPreviewFormats(Camera.Parameters parameters) {
        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        return supportedPreviewFormats;
    }

    /**
     * 设置对焦，会影响camera吞吐速率
     */
    public static void setFocusModes(Camera.Parameters parameters) {
        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        }
    }

    /**
     * 设置fps
     */
    public static void chooseFrameRate(Camera.Parameters parameters, float frameRate) {
        int frameRat = (int) (frameRate * 1000);
        List<int[]> rates = parameters.getSupportedPreviewFpsRange();
        int[] bestFrameRate = rates.get(0);
        for (int i = 0; i < rates.size(); i++) {
            int[] rate = rates.get(i);
            if (DEBUG) {
                Log.d(TAG, "supported preview pfs min " + rate[0] + " max " + rate[1]);
            }
            int curDelta = Math.abs(rate[1] - frameRat);
            int bestDelta = Math.abs(bestFrameRate[1] - frameRat);
            if (curDelta < bestDelta) {
                bestFrameRate = rate;
            } else if (curDelta == bestDelta) {
                bestFrameRate = bestFrameRate[0] < rate[0] ? rate : bestFrameRate;
            }
        }
        if (DEBUG) {
            Log.d(TAG, "closet frame rate min " + bestFrameRate[0] + " max " + bestFrameRate[1]);
        }
        parameters.setPreviewFpsRange(bestFrameRate[0], bestFrameRate[1]);
    }

    /**
     * Attempts to find a preview size that matches the provided width and height (which
     * specify the dimensions of the encoded video).  If it fails to find a match it just
     * uses the default preview size for video.
     * <p>
     * TODO: should do a best-fit match, e.g.
     * https://github.com/commonsguy/cwac-camera/blob/master/camera/src/com/commonsware/cwac/camera/CameraUtils.java
     */
    public static int[] choosePreviewSize(Camera.Parameters parms, int width, int height) {
        // We should make sure that the requested MPEG size is less than the preferred
        // size, and has the same aspect ratio.
        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (DEBUG) {
                Log.d(TAG, "supported: " + size.width + "x" + size.height);
            }
        }

        for (Camera.Size size : parms.getSupportedPreviewSizes()) {
            if (size.width == width && size.height == height) {
                parms.setPreviewSize(width, height);
                if (DEBUG) {
                    Log.d(TAG, "setPreviewSize " + width + "x" + height);
                }
                return new int[]{width, height};
            }
        }

        if (DEBUG) {
            Log.w(TAG, "Unable to set preview size to " + width + "x" + height);
        }
        Camera.Size ppsfv = parms.getPreferredPreviewSizeForVideo();
        if (ppsfv != null) {
            parms.setPreviewSize(ppsfv.width, ppsfv.height);
            return new int[]{ppsfv.width, ppsfv.height};
        }
        // else use whatever the default size is
        return new int[]{0, 0};
    }
}
