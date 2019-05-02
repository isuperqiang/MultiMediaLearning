package com.richie.multimedialearning.utils.wav;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 将pcm文件转化为wav文件
 */
public final class PcmToWav {

    public static boolean makePcmFileToWavFile(String srcPcmPath, String destWavPath, boolean deletePcmFile) {
        return makePcmFileToWavFile(srcPcmPath, destWavPath, deletePcmFile,
                1, 44100, 16);
    }

    /**
     * 将pcm文件转化为wav文件
     *
     * @param srcPcmPath    源 pcm 文件路径
     * @param destWavPath   目标 wav 文件路径
     * @param deletePcmFile 是否删除源文件
     * @return
     */
    public static boolean makePcmFileToWavFile(String srcPcmPath, String destWavPath, boolean deletePcmFile,
                                               int numChannels, int sampleRate, int bitPerSample) {
        File file = new File(srcPcmPath);
        if (!file.exists()) {
            return false;
        }

        int totalSize = (int) file.length();
        WaveHeader header = new WaveHeader();
        header.ChunkSize = totalSize + 36; // 该区块数据的长度（不包含ID和Size的长度）
        header.AudioFormat = 1; // pcm 音频数据
        header.NumChannels = (short) numChannels; // 单通道 1，双通道 2
        header.SampleRate = sampleRate; // 采样率
        header.BitsPerSample = bitPerSample; // 位宽 16 位
        header.Subchunk2Size = totalSize; // 音频数据的长度

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e) {
            return false;
        }

        if (h.length != 44) { // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }

        File destFile = new File(destWavPath);
        if (destFile.exists()) {
            destFile.delete();
        }

        InputStream inStream = null;
        OutputStream outStream = null;
        byte[] buffer = new byte[8196];
        try {
            outStream = new BufferedOutputStream(new FileOutputStream(destWavPath));
            outStream.write(h);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            while (size != -1) {
                outStream.write(buffer, 0, size);
                size = inStream.read(buffer);
            }
        } catch (IOException ioe) {
            return false;
        } finally {
            if (outStream != null) {
                try {
                    outStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
        if (deletePcmFile) {
            file.delete();
        }
        return true;
    }


    public static boolean makePcmStreamToWavFile(byte[] pcmBytes, String destWavPath) {
        return makePcmStreamToWavFile(pcmBytes, destWavPath, 1, 16000, 16);
    }

    /**
     * 将pcm流转化为wav文件
     *
     * @param pcmBytes    pcm字节流
     * @param destWavPath 目标文件路径
     * @return
     */
    public static boolean makePcmStreamToWavFile(byte[] pcmBytes, String destWavPath, int numChannels, int sampleRate, int bitPerSample) {
        int totalSize = pcmBytes.length;
        WaveHeader header = new WaveHeader();
        header.ChunkSize = totalSize + 36; // 该区块数据的长度（不包含ID和Size的长度）
        header.AudioFormat = 1; // pcm 音频数据
        header.NumChannels = (short) numChannels; // 单通道 1，双通道 2
        header.SampleRate = sampleRate; // 采样率
        header.BitsPerSample = bitPerSample; // 位宽 16 位
        header.Subchunk2Size = totalSize; // 音频数据的长度

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e) {
            return false;
        }

        if (h.length != 44) { // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }

        File destFile = new File(destWavPath);
        if (destFile.exists()) {
            destFile.delete();
        }

        InputStream inStream = null;
        OutputStream ouStream = null;
        byte[] buffer = new byte[8196];
        try {
            ouStream = new BufferedOutputStream(new FileOutputStream(destWavPath));
            ouStream.write(h);
            inStream = new BufferedInputStream(new ByteArrayInputStream(pcmBytes));
            int size = inStream.read(buffer);
            while (size != -1) {
                ouStream.write(buffer, 0, size);
                size = inStream.read(buffer);
            }
        } catch (IOException ioe) {
            return false;
        } finally {
            if (ouStream != null) {
                try {
                    ouStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    // ignored
                }
            }
        }
        return true;
    }

}
