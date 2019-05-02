package com.richie.multimedialearning.utils.wav;

import com.richie.multimedialearning.utils.ConvertUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * @author Richie on 2019.04.16
 */
public final class WavUtils {

    /**
     * 检索 WAV 文件的头信息
     *
     * @param wavFile
     * @return
     * @throws IOException
     */
    public static WaveHeader retrieveHeader(File wavFile) throws IOException {
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(wavFile))) {
            byte[] headerBytes = new byte[44];
            int readLength = bis.read(headerBytes, 0, headerBytes.length);
            if (headerBytes.length != readLength) {
                // wrong wav file
                throw new IOException("Wrong wav format!");
            }
            byte[] tempSize2 = new byte[2];
            byte[] tempSize4 = new byte[4];
            WaveHeader waveHeader = new WaveHeader();
            System.arraycopy(headerBytes, 4, tempSize4, 0, tempSize4.length);
            waveHeader.ChunkSize = ConvertUtils.fromByteArrayToInt(tempSize4);
            System.arraycopy(headerBytes, 16, tempSize4, 0, tempSize4.length);
            waveHeader.Subchunk1Size = ConvertUtils.fromByteArrayToInt(tempSize4);
            System.arraycopy(headerBytes, 20, tempSize2, 0, tempSize2.length);
            waveHeader.AudioFormat = ConvertUtils.fromByteArrayToShort(tempSize2);
            System.arraycopy(headerBytes, 22, tempSize2, 0, tempSize2.length);
            waveHeader.NumChannels = ConvertUtils.fromByteArrayToShort(tempSize2);
            System.arraycopy(headerBytes, 24, tempSize4, 0, tempSize4.length);
            waveHeader.SampleRate = ConvertUtils.fromByteArrayToInt(tempSize4);
            System.arraycopy(headerBytes, 28, tempSize4, 0, tempSize4.length);
            waveHeader.BitsRate = ConvertUtils.fromByteArrayToInt(tempSize4);
            System.arraycopy(headerBytes, 32, tempSize2, 0, tempSize2.length);
            waveHeader.BlockAlign = ConvertUtils.fromByteArrayToShort(tempSize2);
            System.arraycopy(headerBytes, 34, tempSize2, 0, tempSize2.length);
            waveHeader.BitsPerSample = ConvertUtils.fromByteArrayToShort(tempSize2);
            System.arraycopy(headerBytes, 40, tempSize4, 0, tempSize4.length);
            waveHeader.Subchunk2Size = ConvertUtils.fromByteArrayToInt(tempSize4);
            return waveHeader;
        }
    }
}
