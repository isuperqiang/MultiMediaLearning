package com.richie.multimedialearning.utils.gles.program;

import android.opengl.GLES20;

import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.drawable.Drawable2d;

/**
 * Base class for GL program
 */
public abstract class Program {

    // Handles to the GL program and various components of it.
    protected int mProgramHandle;
    protected Drawable2d mDrawable2d;

    public Program(String vertexShader, String fragmentShader) {
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
        mDrawable2d = createDrawable2d();
        glGetLocations();
    }

    /**
     * Draw frame in identity mvp matrix
     *
     * @param textureId
     * @param texMatrix
     */
    public void drawFrame(int textureId, float[] texMatrix) {
        drawFrame(textureId, texMatrix, GlUtil.IDENTITY_MATRIX);
    }

    /**
     * Draw frame in specified area
     *
     * @param textureId
     * @param texMatrix
     * @param mvpMatrix
     * @param x         viewport x
     * @param y         viewport y
     * @param width     viewport width
     * @param height    viewport height
     */
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix, int x, int y, int width, int height) {
        int[] originalViewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, originalViewport, 0);
        GLES20.glViewport(x, y, width, height);
        drawFrame(textureId, texMatrix, mvpMatrix);
        GLES20.glViewport(originalViewport[0], originalViewport[1], originalViewport[2], originalViewport[3]);
    }

    /**
     * Releases the program.
     * <p>
     * The appropriate EGL context must be current (i.e. the one that was used to create
     * the program).
     */
    public void release() {
        GLES20.glDeleteProgram(mProgramHandle);
        mProgramHandle = -1;
    }

    /**
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param textureId texture ID
     * @param mvpMatrix The 4x4 projection matrix.
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *                  for use with SurfaceTexture.)
     */
    public abstract void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix);

    /**
     * Create drawable2d that OpenGL will use
     *
     * @return
     */
    protected abstract Drawable2d createDrawable2d();

    /**
     * Call GLES20.glGetXXXLocation method to get location of attributes and uniforms
     */
    protected abstract void glGetLocations();

}
