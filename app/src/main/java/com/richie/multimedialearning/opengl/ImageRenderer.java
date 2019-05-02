package com.richie.multimedialearning.opengl;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2018.08.06
 * 绘制图片
 */
public class ImageRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "attribute vec2 aTexCoord;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "  vTexCoord = aTexCoord;" +
                    "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform sampler2D uTextureUnit;" +
                    "varying vec2 vTexCoord;" +
                    "void main() {" +
                    "  gl_FragColor = texture2D(uTextureUnit, vTexCoord);" +
                    "}";
    private static final int COORDS_PER_VERTEX = 2;
    private static final int COORDS_PER_TEXTURE = 2;
    private static final float[] VERTEX = {
            1, 1,  // top right
            -1, 1, // top left
            1, -1, // bottom right
            -1, -1,// bottom left
    };
    private static final float[] TEXTURE = {
            1, 0,  // top right
            0, 0,  // top left
            1, 1,  // bottom right
            0, 1,  // bottom left
    };
    private final FloatBuffer mVertexBuffer;
    private final FloatBuffer mTextureBuffer;
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private float[] mMvpMatrix = new float[16];
    private int mProgram;
    private int mMvpMatrixHandle;
    private int mPositionHandle;
    private int mTexCoordHandle;
    private int mBitmapWidth;
    private int mBitmapHeight;
    private String mImagePath;

    public ImageRenderer(String imagePath) {
        mImagePath = imagePath;
        mVertexBuffer = GLESUtils.createFloatBuffer(VERTEX);
        mTextureBuffer = GLESUtils.createFloatBuffer(TEXTURE);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        int vertexShader = GLESUtils.createVertexShader(VERTEX_SHADER);
        int fragmentShader = GLESUtils.createFragmentShader(FRAGMENT_SHADER);
        mProgram = GLESUtils.createProgram(vertexShader, fragmentShader);
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        mMvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        mTexCoordHandle = GLES20.glGetAttribLocation(mProgram, "aTexCoord");
        int texUnitHandle = GLES20.glGetAttribLocation(mProgram, "uTextureUnit");

        // 加载图片并且保存在 OpenGL 纹理系统中
        Bitmap bitmap = BitmapFactory.decodeFile(mImagePath);
        mBitmapWidth = bitmap.getWidth();
        mBitmapHeight = bitmap.getHeight();
        int imageTexture = GLESUtils.createImageTexture(bitmap);
        // 激活纹理
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        // 绑定纹理
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, imageTexture);
        // 把选定的纹理单元传给片段着色器
        GLES20.glUniform1i(texUnitHandle, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);

        mMvpMatrix = GLESUtils.changeMvpMatrixInside(width, height, mBitmapWidth, mBitmapHeight);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        GLES20.glUseProgram(mProgram);
        GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1, false, mMvpMatrix, 0);
        // 传入顶点坐标
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                0, mVertexBuffer);
        // 传入纹理坐标
        GLES20.glEnableVertexAttribArray(mTexCoordHandle);
        GLES20.glVertexAttribPointer(mTexCoordHandle, COORDS_PER_TEXTURE, GLES20.GL_FLOAT, false,
                0, mTextureBuffer);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, VERTEX.length / COORDS_PER_VERTEX);
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glDisableVertexAttribArray(mTexCoordHandle);
        GLES20.glUseProgram(0);
    }

}
