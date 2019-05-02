package com.richie.multimedialearning.utils;

import android.text.TextUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * @author Richie on 2018.11.26
 */
public class ConvertUtils {

    private ConvertUtils() {
    }

    public static int fromByteArrayToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt();
    }

    public static short fromByteArrayToShort(byte[] bytes) {
        return ByteBuffer.wrap(bytes)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getShort();
    }

    public static int byteArrayToInt(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    public static byte[] int2ByteArray(int a) {
        byte[] ret = new byte[4];
        ret[3] = (byte) (a & 0xFF);
        ret[2] = (byte) ((a >> 8) & 0xFF);
        ret[1] = (byte) ((a >> 16) & 0xFF);
        ret[0] = (byte) ((a >> 24) & 0xFF);
        return ret;
    }

    public static float convertString2Float(String s) {
        float ret = 0f;
        if (!TextUtils.isEmpty(s)) {
            try {
                ret = Float.parseFloat(s);
            } catch (NumberFormatException e) {
                // ignored
            }
        }
        return ret;
    }

    public static int convertString2Integer(String s) {
        int ret = 0;
        if (!TextUtils.isEmpty(s)) {
            try {
                ret = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                // ignored
            }
        }
        return ret;
    }
}
