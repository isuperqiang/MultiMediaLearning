package com.richie.multimedialearning.utils.wav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * wav文件头
 */
class WaveHeader {
    private static final char FILE_ID[] = {'R', 'I', 'F', 'F'};
    private static final char WAV_TAG[] = {'W', 'A', 'V', 'E'};
    private static final char DATA_HDRID[] = {'d', 'a', 't', 'a'};
    private static final char FMT_HDRID[] = {'f', 'm', 't', ' '};
    public int fileLength;
    public int fmthdrleth;
    public short formatTag;
    public short channels;
    public int samplesPerSec;
    public int avgBytesPerSec;
    public short blockAlign;
    public short bitsPerSample;
    public int dataHdrLeth;

    public byte[] getHeader() throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            bos = new ByteArrayOutputStream();
            writeChar(bos, FILE_ID);
            writeInt(bos, fileLength);
            writeChar(bos, WAV_TAG);
            writeChar(bos, FMT_HDRID);
            writeInt(bos, fmthdrleth);
            writeShort(bos, formatTag);
            writeShort(bos, channels);
            writeInt(bos, samplesPerSec);
            writeInt(bos, avgBytesPerSec);
            writeShort(bos, blockAlign);
            writeShort(bos, bitsPerSample);
            writeChar(bos, DATA_HDRID);
            writeInt(bos, dataHdrLeth);
            bos.flush();
            byte[] r = bos.toByteArray();
            return r;
        } finally {
            if (bos != null) {
                bos.close();
            }
        }
    }

    private void writeShort(ByteArrayOutputStream bos, int s) throws IOException {
        byte[] bytes = new byte[2];
        bytes[1] = (byte) ((s << 16) >> 24);
        bytes[0] = (byte) ((s << 24) >> 24);
        bos.write(bytes);
    }

    private void writeInt(ByteArrayOutputStream bos, int n) throws IOException {
        byte[] buf = new byte[4];
        buf[3] = (byte) (n >> 24);
        buf[2] = (byte) ((n << 8) >> 24);
        buf[1] = (byte) ((n << 16) >> 24);
        buf[0] = (byte) ((n << 24) >> 24);
        bos.write(buf);
    }

    private void writeChar(ByteArrayOutputStream bos, char[] id) {
        for (char c : id) {
            bos.write(c);
        }
    }
}
