package com.richie.multimedialearning.mediacodec;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.opengl.CameraHolder;
import com.richie.multimedialearning.opengl.GLESUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2019.05.05
 */
public class CameraRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
            "attribute vec4 aPosition;" + // 标准化设备坐标点(NDC) 坐标点
                    "attribute vec4 aTextureCoord;" + // 纹理坐标点
                    "uniform mat4 uMVPMatrix;" + // NDC MVP 变换矩阵
                    "uniform mat4 uTexMatrix;" + // 纹理坐标变换矩阵
                    "varying vec2 vTextureCoord;" + // 纹理坐标点变换后输出
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" + // 对 NDC 坐标点进行变换
                    "  vTextureCoord = (uTexMatrix * aTextureCoord).xy;" + // 对纹理坐标点进行变换
                    "}";
    private static final String FRAGMENT_SHADER =
            "#extension GL_OES_EGL_image_external : require\n" + // 声明OES纹理使用扩展
                    "precision mediump float;" + // 声明精度
                    "varying vec2 vTextureCoord;" + // 顶点着色器输出经图元装配和栅格化的纹理坐标点序列
                    "uniform samplerExternalOES sTexture;" + // OES 纹理，接收相机纹理作为输入
                    "void main() {" +
                    "  gl_FragColor = texture2D(sTexture, vTextureCoord);" +
                    "}";
    // 顶点坐标
    private static final float[] VERTEX = {   // in counterclockwise order:
            1, 1,   // top right
            -1, 1,  // top left
            -1, -1, // bottom left
            1, -1,   // bottom right
    };
    // 纹理坐标
    private static final float[] TEXTURE = {   // in counterclockwise order:
            1, 0,  // top right
            0, 0,  // top left
            0, 1,  // bottom left
            1, 1,  // bottom right
    };
    private final ILogger logger = LoggerFactory.getLogger(CameraRenderer.class);
    // 程序句柄
    private int mProgram;
    // 顶点坐标Buffer
    private FloatBuffer mVerBuffer;
    // 纹理坐标Buffer
    private FloatBuffer mTexBuffer;
    // 纹理句柄
    private int mTextureID;
    // 顶点坐标 MVP 变换矩阵
    private float[] mMvpMatrix = new float[16];
    // 纹理坐标变换矩阵
    private float[] mTexMatrix = new float[16];
    // 用于存储回调数据的buffer
    private ByteBuffer[] mOutPutBuffer = new ByteBuffer[2];
    // 回调数据使用的buffer索引
    private int mIndexOutput = 0;
    private int mMvpMatrixHandle;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mTexMatrixHandle;
    private SurfaceTexture mSurfaceTexture;
    private CameraHolder mCameraHolder;
    private Activity mActivity;

    public CameraRenderer(Activity activity) {
        mActivity = activity;
        mCameraHolder = new CameraHolder();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        logger.debug("onSurfaceCreated() called");
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
        mVerBuffer = GLESUtils.createFloatBuffer(VERTEX);
        mTexBuffer = GLESUtils.createFloatBuffer(TEXTURE);
        mTextureID = GLESUtils.createOESTexture();
        mSurfaceTexture = new SurfaceTexture(mTextureID);
        int vertexShader = GLESUtils.createShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER);
        int fragmentShader = GLESUtils.createShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER);
        mProgram = GLESUtils.createProgram(vertexShader, fragmentShader);
        logger.info("textureId:{}, vertexShader:{}, fragmentShader:{}, program:{}", mTextureID,
                vertexShader, fragmentShader, mProgram);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTextureCoord");
        mMvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTexMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uTexMatrix");
        int texUnitHandle = GLES20.glGetAttribLocation(mProgram, "sTexture");
        GLES20.glUniform1i(texUnitHandle, 0);

        //mSurfaceTexture.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
        //    @Override
        //    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //
        //    }
        //});
        mCameraHolder.openCamera(mActivity);
        mCameraHolder.setPreviewTexture(mSurfaceTexture);
        mCameraHolder.startPreview();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        logger.debug("onSurfaceChanged() called. width:{}, height:{}", width, height);
        GLES20.glViewport(0, 0, width, height);
        mMvpMatrix = GLESUtils.changeMvpMatrixCrop(width, height, CameraHolder.PREVIEW_HEIGHT, CameraHolder.PREVIEW_WIDTH);
        GLESUtils.rotate(mMvpMatrix, 180);
        GLESUtils.flip(mMvpMatrix, true, false);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTexMatrix);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureID);
        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1, false, mMvpMatrix, 0);
        GLES20.glUniformMatrix4fv(mTexMatrixHandle, 1, false, mTexMatrix, 0);

        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, 2, GLES20.GL_FLOAT, false, 0, mVerBuffer);
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, 2, GLES20.GL_FLOAT, false, 0, mTexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, 4);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0);
        GLES20.glUseProgram(0);
    }

    public void onStop() {
        mCameraHolder.stopPreview();
        mCameraHolder.release();
    }

}
