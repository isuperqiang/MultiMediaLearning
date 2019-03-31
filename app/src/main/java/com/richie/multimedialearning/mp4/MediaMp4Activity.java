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
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * 音视频混合和分离
 */
public class MediaMp4Activity extends AppCompatActivity implements View.OnClickListener {
    public static final String OUTPUT_VIDEO = "output_video.mp4";
    public static final String OUTPUT_AUDIO = "output_audio.mp3";
    private static final String VIDEO_SOURCE_PATH = "input.mp4";
    private final ILogger logger = LoggerFactory.getLogger(MediaMp4Activity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_mp4);
        findViewById(R.id.btn_extract_video).setOnClickListener(this);
        findViewById(R.id.btn_extract_audio).setOnClickListener(this);
        findViewById(R.id.btn_mix_media).setOnClickListener(this);
        checkInputFile();
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

    private void checkInputFile() {
        try {
            File dest = new File(FileUtils.getFileDir(this), VIDEO_SOURCE_PATH);
            if (!dest.exists()) {
                InputStream inputStream = getAssets().open(VIDEO_SOURCE_PATH);
                FileUtils.copyFile(inputStream, dest);
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    // 分离视频的纯视频 输入视频 input.mp4，分离保存的视频 output_video.mp4
    private void extractVideo() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            File fileDir = FileUtils.getFileDir(this);
            // 设置视频源
            mediaExtractor.setDataSource(new File(fileDir, VIDEO_SOURCE_PATH).getAbsolutePath());
            int videoIndex = -1;
            int maxInputSize = -1;
            int frameRate = -1;
            // 获取数据源的轨道数
            int trackCount = mediaExtractor.getTrackCount();
            // 循环轨道数，找到我们想要的视频轨
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                // 主要描述mime类型的媒体格式
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                // //找到视频轨
                if (mimeType.startsWith("video/")) {
                    logger.info("video mimeType:{}", mimeType);
                    videoIndex = i;
                    // 获取视频最大的输入大小
                    maxInputSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    // 获取视频的帧率
                    frameRate = trackFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                }
            }
            logger.info("videoIndex:{}, frameRate:{}, maxInputSize:{}", videoIndex, frameRate, maxInputSize);
            //切换视频的信道
            mediaExtractor.selectTrack(videoIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(videoIndex);
            mediaMuxer = new MediaMuxer(new File(fileDir, OUTPUT_VIDEO).getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            //将视频轨添加到 MediaMuxer，并返回新的轨道
            int trackIndex = mediaMuxer.addTrack(trackFormat);
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
                bufferInfo.presentationTimeUs += 1000 * 1000 / frameRate;
                //写入样本数据
                mediaMuxer.writeSampleData(trackIndex, byteBuffer, bufferInfo);
                //推进到下一个样本，类似快进
                mediaExtractor.advance();
            }

            logger.info("finish extract video");
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

    // 分离视频的纯音频, 输入视频是 input.mp4, 输出的音频为 output_audio.mp3
    private void extractAudio() {
        MediaExtractor mediaExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        int audioIndex = -1;
        try {
            File fileDir = FileUtils.getFileDir(this);
            mediaExtractor.setDataSource(new File(fileDir, VIDEO_SOURCE_PATH).getAbsolutePath());
            int trackCount = mediaExtractor.getTrackCount();
            int frameMaxInputSize = -1;
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String mimeType = trackFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    logger.info("audio mimeType:{}", mimeType);
                    audioIndex = i;
                    frameMaxInputSize = trackFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                }
            }

            mediaExtractor.selectTrack(audioIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioIndex);
            mediaMuxer = new MediaMuxer(new File(fileDir, OUTPUT_AUDIO).getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeAudioIndex = mediaMuxer.addTrack(trackFormat);
            mediaMuxer.start();
            ByteBuffer byteBuffer = ByteBuffer.allocate(frameMaxInputSize);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
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

            logger.info("finish extract audio");
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
    // 输入视频 output_video.mp4，输入音频是 output_audio.mp4 合成视频output.mp4
    private void combineVideo() {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaExtractor audioExtractor = new MediaExtractor();
        MediaMuxer mediaMuxer = null;
        try {
            File fileDir = FileUtils.getFileDir(this);
            videoExtractor.setDataSource(new File(fileDir, OUTPUT_VIDEO).getAbsolutePath());
            MediaFormat videoFormat = null;
            int videoTrackIndex = -1;
            int videoMaxInputSize = -1;
            int frameRate = -1;
            int videoTrackCount = videoExtractor.getTrackCount();
            for (int i = 0; i < videoTrackCount; i++) {
                videoFormat = videoExtractor.getTrackFormat(i);
                String mimeType = videoFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("video/")) {
                    videoTrackIndex = i;
                    videoMaxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                    break;
                }
            }

            audioExtractor.setDataSource(new File(fileDir, OUTPUT_AUDIO).getAbsolutePath());
            MediaFormat audioFormat = null;
            int audioTrackIndex = -1;
            int audioMaxInputSize = -1;
            int audioTrackCount = audioExtractor.getTrackCount();
            for (int i = 0; i < audioTrackCount; i++) {
                audioFormat = audioExtractor.getTrackFormat(i);
                String mimeType = audioFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    audioTrackIndex = i;
                    audioMaxInputSize = audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE);
                    break;
                }
            }

            mediaMuxer = new MediaMuxer(new File(fileDir, "output.mp4").getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            int writeVideoTrackIndex = mediaMuxer.addTrack(videoFormat);
            int writeAudioTrackIndex = mediaMuxer.addTrack(audioFormat);
            mediaMuxer.start();

            videoExtractor.selectTrack(videoTrackIndex);
            MediaCodec.BufferInfo videoBufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer videoByteBuffer = ByteBuffer.allocate(videoMaxInputSize);
            while (true) {
                int readVideoSampleSize = videoExtractor.readSampleData(videoByteBuffer, 0);
                if (readVideoSampleSize < 0) {
                    videoExtractor.unselectTrack(videoTrackIndex);
                    break;
                }
                videoBufferInfo.size = readVideoSampleSize;
                videoBufferInfo.presentationTimeUs += 1000 * 1000 / frameRate;
                videoBufferInfo.offset = 0;
                videoBufferInfo.flags = videoExtractor.getSampleFlags();
                mediaMuxer.writeSampleData(writeVideoTrackIndex, videoByteBuffer, videoBufferInfo);
                videoExtractor.advance();
            }

            audioExtractor.selectTrack(audioTrackIndex);
            MediaCodec.BufferInfo audioBufferInfo = new MediaCodec.BufferInfo();
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

            logger.info("finish muxer media");
            Toast.makeText(this, "混合音视频完成", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (mediaMuxer != null) {
                mediaMuxer.stop();
                mediaMuxer.release();
            }
            videoExtractor.release();
            audioExtractor.release();
        }
    }
}
