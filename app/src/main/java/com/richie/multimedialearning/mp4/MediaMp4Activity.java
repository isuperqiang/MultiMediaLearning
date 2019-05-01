package com.richie.multimedialearning.mp4;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.R;
import com.richie.multimedialearning.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 音视频混合和分离
 * https://mp.weixin.qq.com/s?timestamp=1556713288&src=3&ver=1&signature=O*pikadNlafbj88qn-Qy4*TJlpfOJfe65DikvyxR6Q4lJEGfOAtxsI6eBie1XnGGLt6ON0aNdGWnw4E-Kbfqv6mZGJckaCioc-PmeZxygVUzu7*ec8CxORCd3WcX5bAgHbJ1AnIN1WTGLAj*v9lJwkJ9qmJm6SxmshqE9T*6a9M=
 */
public class MediaMp4Activity extends AppCompatActivity implements View.OnClickListener {
    public static final String OUTPUT_VIDEO = "output_video.mp4";
    public static final String OUTPUT_AUDIO = "output_audio.mp3";
    private static final String VIDEO_SOURCE = "input.mp4";
    private final ILogger logger = LoggerFactory.getLogger(MediaMp4Activity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_mp4);
        findViewById(R.id.btn_extract_video).setOnClickListener(this);
        findViewById(R.id.btn_extract_audio).setOnClickListener(this);
        findViewById(R.id.btn_mix_media).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_extract_audio:
                extractAudio();
                break;
            case R.id.btn_extract_video:
                extractVideo();
                break;
            case R.id.btn_mix_media:
                combineVideo();
                break;
            default:
        }
    }

    // 分离视频的视频轨，输入视频 input.mp4，输出视频 output_video.mp4
    private void extractVideo() {
        logger.info("extractVideo() start");
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            File fileDir = FileUtils.getFileDir(this);
            // 设置视频源
            mediaExtractor.setDataSource(new File(fileDir, VIDEO_SOURCE).getAbsolutePath());
            // 轨道索引 ID
            int videoIndex = -1;
            // 视频轨道格式信息
            MediaFormat mediaFormat = null;
            // 数据源的轨道数（一般有视频，音频，字幕等）
            int trackCount = mediaExtractor.getTrackCount();
            // 循环轨道数，找到我们想要的视频轨
            for (int i = 0; i < trackCount; i++) {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                // //找到要分离的视频轨
                if (mimeType.startsWith("video/")) {
                    videoIndex = i;
                    break;
                }
            }
            if (mediaFormat == null || videoIndex < 0) {
                return;
            }

            // 最大缓冲区字节数
            int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            // 格式类型
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            // 视频的比特率
            int bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
            // 视频宽度
            int width = mediaFormat.getInteger(MediaFormat.KEY_WIDTH);
            // 视频高度
            int height = mediaFormat.getInteger(MediaFormat.KEY_HEIGHT);
            // 内容持续时间（以微妙为单位）
            long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
            // 视频的帧率
            int frameRate = mediaFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
            // 视频内容颜色空间
            int colorFormat = -1;
            if (mediaFormat.containsKey(MediaFormat.KEY_COLOR_FORMAT)) {
                mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT);
            }
            // 关键之间的时间间隔
            int iFrameInterval = -1;
            if (mediaFormat.containsKey(MediaFormat.KEY_I_FRAME_INTERVAL)) {
                iFrameInterval = mediaFormat.getInteger(MediaFormat.KEY_I_FRAME_INTERVAL);
            }
            //  视频旋转顺时针角度
            int rotation = -1;
            if (mediaFormat.containsKey(MediaFormat.KEY_ROTATION)) {
                rotation = mediaFormat.getInteger(MediaFormat.KEY_ROTATION);
            }
            // 比特率模式
            int bitRateMode = -1;
            if (mediaFormat.containsKey(MediaFormat.KEY_BITRATE_MODE)) {
                bitRateMode = mediaFormat.getInteger(MediaFormat.KEY_BITRATE_MODE);
            }

            logger.info("mimeType:{}, maxInputSize:{}, bitRate:{}, width:{}, height:{}" +
                            ", duration:{}ms, frameRate:{}, colorFormat:{}, iFrameInterval:{}" +
                            ", rotation:{}, bitRateMode:{}", mimeType, maxInputSize, bitRate, width, height
                    , duration / 1000, frameRate, colorFormat, iFrameInterval, rotation, bitRateMode);
            //切换视频的轨道
            mediaExtractor.selectTrack(videoIndex);

            String outPath = new File(fileDir, OUTPUT_VIDEO).getAbsolutePath();
            mediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //将视频轨添加到 MediaMuxer，并返回新的轨道
            int trackIndex = mediaMuxer.addTrack(mediaFormat);
            ByteBuffer byteBuffer = ByteBuffer.allocate(maxInputSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            // 开始合成
            mediaMuxer.start();
            while (true) {
                // 检索当前编码的样本并将其存储在字节缓冲区中
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                //  如果没有可获取的样本则退出循环
                if (readSampleSize < 0) {
                    mediaExtractor.unselectTrack(videoIndex);
                    break;
                }
                // 设置样本编码信息
                bufferInfo.size = readSampleSize;
                bufferInfo.offset = 0;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                //写入样本数据
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
                //推进到下一个样本，类似快进
                mediaExtractor.advance();
            }

            logger.info("finish extract video, path:{}", outPath);
            Toast.makeText(this, "分离视频完成", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }
            mediaExtractor.release();
        }
    }

    // 分离视频的音频轨, 输入视频 input.mp4, 输出的音频 output_audio.mp3
    private void extractAudio() {
        logger.info("extractAudio() start");
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            File fileDir = FileUtils.getFileDir(this);
            mediaExtractor.setDataSource(new File(fileDir, VIDEO_SOURCE).getAbsolutePath());
            int trackCount = mediaExtractor.getTrackCount();
            MediaFormat mediaFormat = null;
            int audioIndex = -1;
            for (int i = 0; i < trackCount; i++) {
                mediaFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    audioIndex = i;
                    break;
                }
            }
            if (mediaFormat == null || audioIndex < 0) {
                return;
            }
            // 最大缓冲区字节数
            int maxInputSize = mediaFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            // 格式
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            // 比特率
            int bitRate = mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE);
            // 通道数
            int channelCount = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            // 采样率
            int sampleRate = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            // 内容持续时间（以微妙为单位）
            long duration = mediaFormat.getLong(MediaFormat.KEY_DURATION);
            logger.info("maxInputSize:{}, mimeType:{}, bitRate:{}, channelCount:{}" +
                            ", sampleRate:{}, duration:{}ms", maxInputSize, mimeType, bitRate, channelCount,
                    sampleRate, duration / 1000);
            mediaExtractor.selectTrack(audioIndex);

            String outPath = new File(fileDir, OUTPUT_AUDIO).getAbsolutePath();
            mediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeAudioIndex = mediaMuxer.addTrack(mediaFormat);
            ByteBuffer byteBuffer = ByteBuffer.allocate(maxInputSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            mediaMuxer.start();
            while (true) {
                int readSampleSize = mediaExtractor.readSampleData(byteBuffer, 0);
                if (readSampleSize < 0) {
                    mediaExtractor.unselectTrack(audioIndex);
                    break;
                }
                bufferInfo.size = readSampleSize;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs = mediaExtractor.getSampleTime();
                mediaMuxer.writeSampleData(writeAudioIndex, byteBuffer, bufferInfo);
                mediaExtractor.advance();
            }

            logger.info("finish extract audio, path:{}", outPath);
            Toast.makeText(this, "分离音频完成", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }
            mediaExtractor.release();
        }
    }

    // 将上面分离出的 output_video.mp4 和分离出 output_audio.mp4 合成原来完整的视频
    // 输入视频 output_video.mp4，输入音频 output_audio.mp4 合成视频 output.mp4
    private void combineVideo() {
        logger.info("combineVideo() start");
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            File fileDir = FileUtils.getFileDir(this);
            videoExtractor.setDataSource(new File(fileDir, OUTPUT_VIDEO).getAbsolutePath());
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    break;
                }
            }
            if (videoFormat == null || videoTrackIndex < 0) {
                return;
            }

            audioExtractor.setDataSource(new File(fileDir, OUTPUT_AUDIO).getAbsolutePath());
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;

            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    break;
                }
            }
            if (audioFormat == null || audioTrackIndex < 0) {
                return;
            }

            String outPath = new File(fileDir, "output.mp4").getAbsolutePath();
            mediaMuxer = new MediaMuxer(outPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();

            videoExtractor.selectTrack(videoTrackIndex);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            int videoMaxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            ByteBuffer videoByteBuffer = ByteBuffer.allocate(videoMaxInputSize);
            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(videoByteBuffer, 0);
                if (readVideoSampleSize < 0) {
                    videoExtractor.unselectTrack(videoTrackIndex);
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs = videoExtractor.getSampleTime();
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, videoByteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }

            audioExtractor.selectTrack(audioTrackIndex);
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
            int audioMaxInputSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
            ByteBuffer audioByteBuffer = ByteBuffer.allocate(audioMaxInputSize);
            while (true) {
                int readAudioSampleSize = audioExtractor.readSampleData(audioByteBuffer, 0);
                if (readAudioSampleSize < 0) {
                    audioExtractor.unselectTrack(audioTrackIndex);
                    break;
                }
                audioBufferInfo.size = readAudioSampleSize;
                audioBufferInfo.presentationTimeUs = audioExtractor.getSampleTime();
                audioBufferInfo.offset = 0;
                audioBufferInfo.flags = audioExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeAudioTrackIndex, audioByteBuffer, audioBufferInfo);
                audioExtractor.advance();
            }

            logger.info("finish muxer media, path:{}", outPath);
            Toast.makeText(this, "混合音视频完成", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (mediaMuxer != null) {
                mediaMuxer.release();
            }
            videoExtractor.release();
            audioExtractor.release();
        }
    }
}
