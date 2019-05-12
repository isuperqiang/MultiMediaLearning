package com.richie.multimedialearning.opengl;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ConfigurationInfo;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.os.AsyncTask;
import android.util.Log;

import com.richie.multimedialearning.utils.gles.GlUtil;
import com.richie.multimedialearning.utils.gles.program.TextureProgram;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.opengles.GL10;

public final class GLESUtils {
    /**
     * Identity matrix for general use.  Don't modify or life will get weird.
     */
    public static final float[] IDENTITY_MATRIX;
    public static final int SIZEOF_FLOAT = 4;
    private static final String TAG = "GLESUtils";

    static {
        IDENTITY_MATRIX = new float[16];
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    private GLESUtils() {
    }

    /**
     * create a vertex shader type (GLES20.GL_VERTEX_SHADER)
     * or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
     *
     * @param type
     * @param shaderCode
     * @return
     */
    public static int createShader(int type, String shaderCode) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderCode);
        // add the source code to the shader and compile it
        GLES20.glCompileShader(shader);
        int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);
        if (compileStatus[0] == 0) {
            Log.e(TAG, "compile shader: " + type + ", error: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }
        return shader;
    }

    public static int createVertexShader(String shaderCode) {
        return createShader(GLES20.GL_VERTEX_SHADER, shaderCode);
    }

    public static int createFragmentShader(String shaderCode) {
        return createShader(GLES20.GL_FRAGMENT_SHADER, shaderCode);
    }

    public static int createProgram(int vertexShader, int fragmentShader) {
        if (vertexShader == 0 || fragmentShader == 0) {
            Log.e(TAG, "shader can't be 0!");
        }
        int program = GLES20.glCreateProgram();
        checkGlError("glCreateProgram");
        if (program == 0) {
            Log.e(TAG, "program can't be 0!");
            return 0;
        }
        GLES20.glAttachShader(program, vertexShader);
        checkGlError("glAttachShader");
        GLES20.glAttachShader(program, fragmentShader);
        checkGlError("glAttachShader");
        GLES20.glLinkProgram(program);
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            Log.e(TAG, "link program error: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            program = 0;
        }
        return program;
    }

    private static void checkGlError(String op) {
        int error = GLES20.glGetError();
        if (error != GLES20.GL_NO_ERROR) {
            String msg = op + ": glError 0x" + Integer.toHexString(error);
            Log.e(TAG, "checkGlError: " + msg);
        }
    }

    public static class ContextFactory implements GLSurfaceView.EGLContextFactory {
        private static int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
            double glVersion = 3.0;
            Log.d(TAG, "creating OpenGL ES " + glVersion + " context");
            int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, (int) glVersion,
                    EGL10.EGL_NONE};
            // attempt to create a OpenGL ES 3.0 context
            EGLContext context = egl.eglCreateContext(
                    display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
            return context; // returns null if 3.0 is not supported;
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {

        }
    }

    // 开发或调试的时候验证
    public static boolean validateProgram(int programId) {
        GLES20.glValidateProgram(programId);
        int[] validateStatus = new int[1];
        GLES20.glGetProgramiv(programId, GLES20.GL_VALIDATE_STATUS, validateStatus, 0);
        return validateStatus[0] != 0;
    }

    // 通过传入图片宽高和预览宽高，计算变换矩阵，得到的变换矩阵是预览类似ImageView的centerCrop效果
    public static void getShowMatrix(float[] matrix, int imgWidth, int imgHeight, int viewWidth, int
            viewHeight) {
        if (imgHeight > 0 && imgWidth > 0 && viewWidth > 0 && viewHeight > 0) {
            float sWhView = (float) viewWidth / viewHeight;
            float sWhImg = (float) imgWidth / imgHeight;
            float[] projection = new float[16];
            float[] camera = new float[16];
            if (sWhImg > sWhView) {
                Matrix.orthoM(projection, 0, -sWhView / sWhImg, sWhView / sWhImg, -1, 1, 1, 3);
            } else {
                Matrix.orthoM(projection, 0, -1, 1, -sWhImg / sWhView, sWhImg / sWhView, 1, 3);
            }
            Matrix.setLookAtM(camera, 0, 0, 0, 5, 0, 0, 0, 0, 1, 0);
            Matrix.multiplyMM(matrix, 0, projection, 0, camera, 0);
        }
    }

    // 旋转
    public static float[] rotate(float[] m, float angle) {
        Matrix.rotateM(m, 0, angle, 0, 0, 3);
        return m;
    }

    // 镜像
    public static float[] flip(float[] m, boolean x, boolean y) {
        if (x || y) {
            Matrix.scaleM(m, 0, x ? -1 : 1, y ? -1 : 1, 1);
        }
        return m;
    }

    // 缩放
    public static float[] scale(float[] m, float x, float y) {
        Matrix.scaleM(m, 0, x, y, 1);
        return m;
    }

    public static FloatBuffer createFloatBuffer(float[] coords) {
        // Allocate a direct ByteBuffer, using 4 bytes per float, and copy coords into it.
        ByteBuffer bb = ByteBuffer.allocateDirect(coords.length * SIZEOF_FLOAT);
        bb.order(ByteOrder.nativeOrder());
        FloatBuffer fb = bb.asFloatBuffer();
        fb.put(coords);
        fb.position(0);
        return fb;
    }

    /**
     * Checks to see if the location we obtained is valid.  GLES returns -1 if a label
     * could not be found, but does not set the GL error.
     * <p>
     * Throws a RuntimeException if the location is invalid.
     */
    public static void checkLocation(int location, String label) {
        if (location < 0) {
            throw new RuntimeException("Unable to locate '" + label + "' in program");
        }
    }

    /**
     * Creates a texture from raw data.
     *
     * @param data   Image data, in a "direct" ByteBuffer.
     * @param width  Texture width, in pixels (not bytes).
     * @param height Texture height, in pixels.
     * @param format Image data format (use constant appropriate for glTexImage2D(), e.g. GL_RGBA).
     * @return Handle to texture.
     */
    public static int createImageTexture(ByteBuffer data, int width, int height, int format) {
        int[] textureHandles = new int[1];
        int textureHandle;

        GLES20.glGenTextures(1, textureHandles, 0);
        textureHandle = textureHandles[0];
        checkGlError("glGenTextures");

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle);

        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        checkGlError("loadImageTexture");

        // Load the data from the buffer into the texture handle.
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, format,
                width, height, 0, format, GLES20.GL_UNSIGNED_BYTE, data);
        checkGlError("loadImageTexture");

        return textureHandle;
    }

    /**
     * Creates a texture object suitable for use with this program.
     * <p>
     * On exit, the texture will be bound.
     *
     * @param textureTarget eg: GLES20.GL_TEXTURE_2D, GLES11Ext.GL_TEXTURE_EXTERNAL_OES
     */
    public static int createTextureObject(int textureTarget) {
        int[] textures = new int[1];
        GLES20.glGenTextures(textures.length, textures, 0);
        GlUtil.checkGlError("glGenTextures");

        int texId = textures[0];
        GLES20.glBindTexture(textureTarget, texId);
        GlUtil.checkGlError("glBindTexture " + texId);

        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(textureTarget, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(textureTarget, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GlUtil.checkGlError("glTexParameter");

        return texId;
    }

    /**
     * Creates a texture from bitmap.
     *
     * @param bmp bitmap data
     * @return Handle to texture.
     */
    public static int createImageTexture(Bitmap bmp) {
        if (bmp == null) {
            return 0;
        }
        int[] textureHandles = new int[1];
        GLES20.glGenTextures(1, textureHandles, 0);
        checkGlError("glGenTextures");
        if (textureHandles[0] == 0) {
            Log.w(TAG, "Could not generate a new OpenGL texture object.");
            return 0;
        }

        // Bind the texture handle to the 2D texture target.
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[0]);
        // Configure min/mag filtering, i.e. what scaling method do we use if what we're rendering
        // is smaller or larger than the source image.
        // 设置缩小过滤为三线性过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
        // 设置放大过滤为双线性过滤
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        // 加载纹理到 OpenGL，读入 Bitmap 定义的位图数据，并把它复制到当前绑定的纹理对象
        // Load the data from the buffer into the texture handle.
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        // 生成 MIP 贴图
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        return textureHandles[0];
    }


    public static void deleteTextureId(int[] textureId) {
        if (textureId != null && textureId.length > 0) {
            GLES20.glDeleteTextures(textureId.length, textureId, 0);
        }
    }

    public static float[] changeMvpMatrixInside(float viewWidth, float viewHeight, float textureWidth, float textureHeight) {
        float scale = viewWidth * textureHeight / viewHeight / textureWidth;
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);
        Matrix.scaleM(mvp, 0, scale > 1 ? (1F / scale) : 1F, scale > 1 ? 1F : scale, 1F);
        return mvp;
    }

    public static float[] changeMvpMatrixCrop(float viewWidth, float viewHeight, float textureWidth, float textureHeight) {
        float scale = viewWidth * textureHeight / viewHeight / textureWidth;
        float[] mvp = new float[16];
        Matrix.setIdentityM(mvp, 0);
        Matrix.scaleM(mvp, 0, scale > 1 ? 1F : (1F / scale), scale > 1 ? scale : 1F, 1F);
        return mvp;
    }

    public static float[] changeMvpMatrix(float[] mvpMatrix, float viewWidth, float viewHeight, float textureWidth, float textureHeight) {
        float scale = viewWidth * textureHeight / viewHeight / textureWidth;
        if (scale == 1) {
            return mvpMatrix;
        } else {
            float[] mvp = new float[16];
            float[] tmp = new float[16];
            Matrix.setIdentityM(tmp, 0);
            Matrix.scaleM(tmp, 0, scale > 1 ? 1F : (1F / scale), scale > 1 ? scale : 1F, 1F);
            Matrix.multiplyMM(mvp, 0, tmp, 0, mvpMatrix, 0);
            return mvp;
        }
    }

    /**
     * glReadPixels, using FBO, the output is bitmap.
     *
     * @param textureId      texture ID
     * @param texMatrix      texture matrix, primarily intended for use with SurfaceTexture.
     * @param mvpMatrix      MVP matrix, usually identity matrix
     * @param texWidth       texture width
     * @param texHeight      texture height
     * @param textureProgram texture oes or 2d texture
     * @param listener       callback when bitmap is available
     */
    public static void glReadBitmap(int textureId, float[] texMatrix, float[] mvpMatrix, final int texWidth,
                                    final int texHeight, TextureProgram textureProgram, final OnReadBitmapListener listener) {
        int[] fboId = new int[1];
        int[] fboTex = new int[1];
        createFBO(fboTex, fboId, texWidth, texHeight);

        //Bind Frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
        //Bind Frame buffer
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex[0]);

        int[] originalViewport = new int[4];
        GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, originalViewport, 0);
        GLES20.glViewport(0, 0, texWidth, texHeight);
        textureProgram.drawFrame(textureId, texMatrix, mvpMatrix);

        final int textureSize = texWidth * texHeight;
        final IntBuffer pixelBuffer = IntBuffer.allocate(textureSize);
        GLES20.glReadPixels(0, 0, texWidth, texHeight, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, pixelBuffer);
        GLES20.glFinish();

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                int[] bitmapSource = new int[textureSize];
                int offset1, offset2;
                int[] data = pixelBuffer.array();
                for (int i = 0; i < texHeight; i++) {
                    offset1 = i * texWidth;
                    offset2 = (texHeight - i - 1) * texWidth;
                    for (int j = 0; j < texWidth; j++) {
                        int texturePixel = data[offset1 + j];
                        int blue = (texturePixel >> 16) & 0x000000ff;
                        int red = (texturePixel << 16) & 0x00ff0000;
                        int pixel = (texturePixel & 0xff00ff00) | red | blue | 0xff000000;
                        bitmapSource[offset2 + j] = pixel;
                    }
                }
                Bitmap bitmap = Bitmap.createBitmap(bitmapSource, texWidth, texHeight, Bitmap.Config.ARGB_8888);
                if (listener != null) {
                    listener.onReadBitmap(bitmap);
                }
            }
        });

        GLES20.glViewport(originalViewport[0], originalViewport[1], originalViewport[2], originalViewport[3]);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        deleteTextureId(fboTex);
        deleteFBO(fboId);
    }

    public interface OnReadBitmapListener {
        /**
         * read bitmap finish
         *
         * @param bitmap
         */
        void onReadBitmap(Bitmap bitmap);
    }

    /**
     * create frame buffer object. (FBO)
     *
     * @param fboTex
     * @param fboId
     * @param width
     * @param height
     */
    public static void createFBO(int[] fboTex, int[] fboId, int width, int height) {
        //generate fbo id
        GLES20.glGenFramebuffers(fboId.length, fboId, 0);
        //generate texture
        GLES20.glGenTextures(fboTex.length, fboTex, 0);

        int texId = createTextureObject(GLES20.GL_TEXTURE_2D);
        fboTex[0] = texId;
        //Bind Frame buffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0]);
        //Define texture parameters
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        //Attach texture FBO color attachment
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, fboTex[0], 0);
        if (GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER) != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            Log.e(TAG, "glFramebufferTexture2D error");
        }
        //we are done, reset
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    public static void deleteFBO(int[] fboId) {
        if (fboId != null && fboId.length > 0) {
            GLES20.glDeleteFramebuffers(fboId.length, fboId, 0);
        }
    }

    public static int getSupportGLVersion(Context context) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo = activityManager.getDeviceConfigurationInfo();
        int version = configurationInfo.reqGlEsVersion >= 0x30000 ? 3 : 2;
        String glEsVersion = configurationInfo.getGlEsVersion();
        Log.e(TAG, "reqGlEsVersion: " + Integer.toHexString(configurationInfo.reqGlEsVersion)
                + ", glEsVersion: " + glEsVersion + ", return: " + version);
        return version;
    }

}
