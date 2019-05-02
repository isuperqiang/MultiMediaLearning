package com.richie.multimedialearning.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;

import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * @author Richie on 2018.08.05
 * 绘制三角形
 */
public class TriangleRenderer implements GLSurfaceView.Renderer {
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;" +
                    "attribute vec4 aPosition;" +
                    "void main() {" +
                    "  gl_Position = uMVPMatrix * aPosition;" +
                    "}";
    private static final String FRAGMENT_SHADER =
            "precision mediump float;" +
                    "uniform vec4 uColor;" +
                    "void main() {" +
                    "  gl_FragColor = uColor;" +
                    "}";
    // Set color with red, green, blue and alpha (opacity) values
    private static final float[] COLORS = {0.8f, 0.5f, 0.3f, 1.0f};
    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;
    // in counterclockwise order:
    private static final float[] COORDS = {
            0, 0.6f, // top
            -0.6f, -0.3f, // bottom left
            0.6f, -0.3f, // bottom right
    };
    private final ILogger logger = LoggerFactory.getLogger(TriangleRenderer.class);
    private FloatBuffer mVertexBuffer;
    private int mProgram;
    private float[] mMvpMatrix = new float[16];
    private float[] mViewMatrix = new float[16];
    private float[] mProjectionMatrix = new float[16];
    private int mColorHandle;
    private int mMvpMatrixHandle;
    private int mPositionHandle;

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        logger.debug("onSurfaceCreated() called");
        // 设置清空屏幕用到的颜色，参数是 Red、Green、Blue、Alpha，范围 [0, 1]。这里我们使用白色背景。
        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);

        mVertexBuffer = GLESUtils.createFloatBuffer(COORDS);
        int vertexShader = GLESUtils.createVertexShader(VERTEX_SHADER);
        int fragmentShader = GLESUtils.createFragmentShader(FRAGMENT_SHADER);
        mProgram = GLESUtils.createProgram(vertexShader, fragmentShader);
        // get handle to fragment shader's uColor member
        mColorHandle = GLES20.glGetUniformLocation(mProgram, "uColor");
        // get handle to vertex shader's aPosition member
        mPositionHandle = GLES20.glGetAttribLocation(mProgram, "aPosition");
        // get handle to shape's transformation matrix
        mMvpMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        logger.debug("onSurfaceChanged() called. width:{}, height:{}", width, height);
        // 设置视口的尺寸，告诉 OpenGL 可以用来渲染的 surface 大小。
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;
        // this projection matrix is applied to object coordinates in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 2.5f, 6);
        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0);
        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMvpMatrix, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        logger.verbose("onDrawFrame() called");
        // 清空屏幕，擦除屏幕上的所有颜色，并用之前 glClearColor 定义的颜色填充整个屏幕。
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Add program to OpenGL ES environment
        GLES20.glUseProgram(mProgram);
        // Enable a handle to the triangle vertices
        GLES20.glEnableVertexAttribArray(mPositionHandle);
        // Prepare the triangle coordinate data
        GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                0, mVertexBuffer);
        // Set color for drawing the triangle
        GLES20.glUniform4fv(mColorHandle, 1, COLORS, 0);
        // Pass the projection and view transformation to the shader
        GLES20.glUniformMatrix4fv(mMvpMatrixHandle, 1, false, mMvpMatrix, 0);
        // Draw the triangle
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, COORDS.length / COORDS_PER_VERTEX);
        // Disable vertex array
        GLES20.glDisableVertexAttribArray(mPositionHandle);
        GLES20.glUseProgram(0);
    }
}
