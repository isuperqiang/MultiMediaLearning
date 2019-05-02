package com.richie.multimedialearning.utils.wav;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * wav文件头
 * https://blog.csdn.net/imxiangzi/article/details/80265978
 */
final class WaveHeader {
    private static final char ChunkID[] = {'R', 'I', 'F', 'F'};
    private static final char Format[] = {'W', 'A', 'V', 'E'};
    private static final char Subchunk1ID[] = {'f', 'm', 't', ' '};
    private static final char Subchunk2ID[] = {'d', 'a', 't', 'a'};
    public int ChunkSize; // 文件的长度减去RIFF区块ChunkID和ChunkSize的长度
    public int Subchunk1Size = 16; // Format区块数据的长度（不包含ID和Size的长度）
    public short AudioFormat; // Data区块的音频数据的格式，PCM音频数据的值为1
    public short NumChannels; // 音频数据的声道数，1：单声道，2：双声道
    public int SampleRate; // 音频数据的采样率
    public int BitsPerSample; // 每个采样点存储的bit数，8，16
    public int BitsRate; // 每秒数据字节数 = SampleRate * NumChannels * BitsPerSample / 8
    public short BlockAlign; // 每个采样点所需的字节数 = NumChannels * BitsPerSample / 8
    public int Subchunk2Size; // 音频数据的长度，N = ByteRate * seconds

    byte[] getHeader() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writeChar(baos, ChunkID);
        writeInt(baos, ChunkSize);
        writeChar(baos, Format);
        writeChar(baos, Subchunk1ID);
        writeInt(baos, Subchunk1Size);
        writeShort(baos, AudioFormat);
        writeShort(baos, NumChannels);
        writeInt(baos, SampleRate);
        BitsRate = NumChannels * BitsPerSample * SampleRate / 8;
        BlockAlign = (short) (NumChannels * BitsPerSample / 8);
        writeInt(baos, BitsRate);
        writeShort(baos, BlockAlign);
        writeShort(baos, BitsPerSample);
        writeChar(baos, Subchunk2ID);
        writeInt(baos, Subchunk2Size);
        baos.flush();
        byte[] bytesHeader = baos.toByteArray();
        baos.close();
        return bytesHeader;
    }

    private void writeShort(ByteArrayOutputStream bos, int s) throws IOException {
        byte[] buf = new byte[2];
        buf[1] = (byte) ((s << 16) >> 24);
        buf[0] = (byte) ((s << 24) >> 24);
        bos.write(buf);
    }

    private void writeInt(ByteArrayOutputStream bos, int i) throws IOException {
        byte[] buf = new byte[4];
        buf[3] = (byte) (i >> 24);
        buf[2] = (byte) ((i << 8) >> 24);
        buf[1] = (byte) ((i << 16) >> 24);
        buf[0] = (byte) ((i << 24) >> 24);
        bos.write(buf);
    }

    private void writeChar(ByteArrayOutputStream bos, char[] chars) {
        for (char c : chars) {
            bos.write(c);
        }
    }
}
