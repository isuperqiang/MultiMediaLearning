package com.richie.multimedialearning.utils.gles.program;

import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.drawable.Drawable2d;
import com.richie.multimedialearning.utils.gles.drawable.Drawable2dFull;

/**
 * GL program and supporting functions for textured 2D shapes.
 */
public class TextureProgram {
    // Simple vertex shader, used for all programs.
    private static final String VERTEX_SHADER =
            "uniform mat4 uMVPMatrix;\n" +
                    "uniform mat4 uTexMatrix;\n" +
                    "attribute vec4 aPosition;\n" +
                    "attribute vec4 aTextureCoord;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "void main() {\n" +
                    "    gl_Position = uMVPMatrix * aPosition;\n" +
                    "    vTextureCoord = (uTexMatrix * aTextureCoord).xy;\n" +
                    "}\n";

    // Simple fragment shader for use with "normal" 2D textures.
    private static final String FRAGMENT_SHADER_2D =
            "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform sampler2D sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    // Simple fragment shader for use with external 2D textures (e.g. what we get from
    // SurfaceTexture).
    private static final String FRAGMENT_SHADER_EXT =
            "#extension GL_OES_EGL_image_external : require\n" +
                    "precision mediump float;\n" +
                    "varying vec2 vTextureCoord;\n" +
                    "uniform samplerExternalOES sTexture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(sTexture, vTextureCoord);\n" +
                    "}\n";

    private int muMVPMatrixLoc;
    private int muTexMatrixLoc;
    private int maPositionLoc;
    private int maTextureCoordLoc;
    private int mTextureTarget;
    // Handles to the GL program and various components of it.
    private int mProgramHandle;
    private Drawable2d mDrawable2d;

    public TextureProgram(String vertexShader, String fragmentShader, int textureTarget) {
        mTextureTarget = textureTarget;
        mProgramHandle = GlUtil.createProgram(vertexShader, fragmentShader);
        mDrawable2d = new Drawable2dFull();
        glGetLocations();
    }

    public static TextureProgram createTexture2D() {
        return new TextureProgram(VERTEX_SHADER, FRAGMENT_SHADER_2D, GLES20.GL_TEXTURE_2D);
    }

    public static TextureProgram createTextureOES() {
        return new TextureProgram(VERTEX_SHADER, FRAGMENT_SHADER_EXT, GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
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
     * Issues the draw call.  Does the full setup on every call.
     *
     * @param textureId texture ID
     * @param mvpMatrix The 4x4 projection matrix.
     * @param texMatrix A 4x4 transformation matrix for texture coords.  (Primarily intended
     *                  for use with SurfaceTexture.)
     */
    public void drawFrame(int textureId, float[] texMatrix, float[] mvpMatrix) {
        GlUtil.checkGlError("draw start");

        // Select the program.
        GLES20.glUseProgram(mProgramHandle);
        GlUtil.checkGlError("glUseProgram");

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, textureId);

        // Copy the model / view / projection matrix over.
        GLES20.glUniformMatrix4fv(muMVPMatrixLoc, 1, false, mvpMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Copy the texture transformation matrix over.
        GLES20.glUniformMatrix4fv(muTexMatrixLoc, 1, false, texMatrix, 0);
        GlUtil.checkGlError("glUniformMatrix4fv");

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(maPositionLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect vertexBuffer to "aPosition".
        GLES20.glVertexAttribPointer(maPositionLoc, Drawable2d.COORDS_PER_VERTEX,
                GLES20.GL_FLOAT, false, Drawable2d.VERTEX_STRIDE, mDrawable2d.getVertexArray());
        GlUtil.checkGlError("glVertexAttribPointer");

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(maTextureCoordLoc);
        GlUtil.checkGlError("glEnableVertexAttribArray");

        // Connect texBuffer to "aTextureCoord".
        GLES20.glVertexAttribPointer(maTextureCoordLoc, 2,
                GLES20.GL_FLOAT, false, Drawable2d.TEX_COORD_STRIDE, mDrawable2d.getTexCoordArray());
        GlUtil.checkGlError("glVertexAttribPointer");

        // Draw the rect.
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mDrawable2d.getVertexCount());
        GlUtil.checkGlError("glDrawArrays");

        // Done -- disable vertex array, texture, and program.
        GLES20.glDisableVertexAttribArray(maPositionLoc);
        GLES20.glDisableVertexAttribArray(maTextureCoordLoc);
        GLES20.glBindTexture(mTextureTarget, 0);
        GLES20.glUseProgram(0);
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

    private void glGetLocations() {
        maPositionLoc = GLES20.glGetAttribLocation(mProgramHandle, "aPosition");
        GlUtil.checkLocation(maPositionLoc, "aPosition");
        maTextureCoordLoc = GLES20.glGetAttribLocation(mProgramHandle, "aTextureCoord");
        GlUtil.checkLocation(maTextureCoordLoc, "aTextureCoord");
        muMVPMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uMVPMatrix");
        GlUtil.checkLocation(muMVPMatrixLoc, "uMVPMatrix");
        muTexMatrixLoc = GLES20.glGetUniformLocation(mProgramHandle, "uTexMatrix");
        GlUtil.checkLocation(muTexMatrixLoc, "uTexMatrix");
    }
}
