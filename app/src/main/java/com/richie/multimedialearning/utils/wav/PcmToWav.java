package com.richie.multimedialearning.utils.wav;

import android.util.Log;

import com.richie.multimedialearning.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by HXL on 16/8/11.
 * 将pcm文件转化为wav文件
 */
public class PcmToWav {
    private static final String TAG = "PcmToWav";

    /**
     * 合并多个pcm文件为一个wav文件
     *
     * @param filePathList    pcm文件路径集合
     * @param destinationPath 目标wav文件路径
     * @return true|false
     */
    public static boolean mergePCMFilesToWAVFile(List<String> filePathList, String destinationPath) {
        int fileNum = filePathList.size();
        File[] file = new File[fileNum];
        int totalSize = 0;

        for (int i = 0; i < fileNum; i++) {
            file[i] = new File(filePathList.get(i));
            totalSize += file[i].length();
        }

        // 填入参数，比特率等等。这里用的是16位单声道 8000 hz
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) +
        // 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = totalSize + (44 - 8);
        header.fmthdrleth = 16;
        header.bitsPerSample = 16;
        header.channels = 2;
        header.formatTag = 0x0001;
        header.samplesPerSec = 8000;
        header.blockAlign = (short) (header.channels * header.bitsPerSample / 8);
        header.avgBytesPerSec = header.blockAlign * header.samplesPerSec;
        header.dataHdrLeth = totalSize;

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            return false;
        }

        if (h.length != 44) { // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }

        //先删除目标文件
        File destFile = new File(destinationPath);
        FileUtils.deleteFileRecursively(destFile);
        //合成所有的pcm文件的数据，写到目标文件
        BufferedOutputStream ouStream = null;
        try {
            ouStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            byte buffer[] = new byte[10240]; // Length of All Files, Total Size
            ouStream.write(h, 0, h.length);
            BufferedInputStream inStream;
            for (int j = 0; j < fileNum; j++) {
                inStream = new BufferedInputStream(new FileInputStream(file[j]));
                int size = inStream.read(buffer);
                while (size != -1) {
                    ouStream.write(buffer);
                    size = inStream.read(buffer);
                }
                inStream.close();
            }
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage());
            return false;
        } finally {
            if (ouStream != null) {
                try {
                    ouStream.close();
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }
        //clearFiles(filePathList);
        Log.i(TAG, "mergePCMFilesToWAVFile  success!");
        return true;
    }

    /**
     * 将一个pcm文件转化为wav文件
     *
     * @param pcmPath         pcm文件路径
     * @param destinationPath 目标文件路径(wav)
     * @param deletePcmFile   是否删除源文件
     * @return
     */
    public static boolean makePCMFileToWAVFile(String pcmPath, String destinationPath, boolean deletePcmFile) {
        File file = new File(pcmPath);
        if (!file.exists()) {
            return false;
        }
        int totalSize = (int) file.length();
        // 填入参数，比特率等等。这里用的是16位单声道 44.1 hz
        WaveHeader header = new WaveHeader();
        // 长度字段 = 内容的大小（TOTAL_SIZE) +
        // 头部字段的大小(不包括前面4字节的标识符RIFF以及fileLength本身的4字节)
        header.fileLength = totalSize + (44 - 8);
        header.fmthdrleth = 16;
        header.bitsPerSample = 16;
        header.channels = 1;
        header.formatTag = 0x0001;
        header.samplesPerSec = 44100;
        header.blockAlign = (short) (header.channels * header.bitsPerSample / 8);
        header.avgBytesPerSec = header.blockAlign * header.samplesPerSec;
        header.dataHdrLeth = totalSize;

        byte[] h;
        try {
            h = header.getHeader();
        } catch (IOException e1) {
            Log.e(TAG, e1.getMessage());
            return false;
        }

        if (h.length != 44) { // WAV标准，头部应该是44字节,如果不是44个字节则不进行转换文件
            return false;
        }

        //先删除目标文件
        File destFile = new File(destinationPath);
        FileUtils.deleteFileRecursively(destFile);

        //合成所有的pcm文件的数据，写到目标文件
        BufferedOutputStream ouStream = null;
        InputStream inStream = null;
        try {
            byte buffer[] = new byte[1024 * 8]; // Length of All Files, Total Size
            ouStream = new BufferedOutputStream(new FileOutputStream(destinationPath));
            ouStream.write(h, 0, h.length);
            inStream = new BufferedInputStream(new FileInputStream(file));
            int size = inStream.read(buffer);
            while (size != -1) {
                ouStream.write(buffer);
                size = inStream.read(buffer);
            }
        } catch (IOException ioe) {
            Log.e(TAG, ioe.getMessage());
            return false;
        } finally {
            if (inStream != null) {
                try {
                    inStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (ouStream != null) {
                try {
                    ouStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (deletePcmFile) {
            FileUtils.deleteFileRecursively(file);
        }
        Log.i(TAG, "makePCMFileToWAVFile  success!");
        return true;
    }
}
