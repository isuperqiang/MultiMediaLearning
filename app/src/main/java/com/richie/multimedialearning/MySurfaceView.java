package com.richie.multimedialearning;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;

/**
 * @author Richie on 2018.10.22
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final ILogger logger = LoggerFactory.getLogger(MySurfaceView.class);
    private volatile boolean mIsDrawing;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private int mWidth;
    private int mHeight;
    private Canvas mCanvas;

    public MySurfaceView(Context context) {
        super(context);
        init();
    }

    public MySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MySurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setColor(Color.RED);
        mPaint.setStrokeWidth(8);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        logger.debug("onSurfaceCreated");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logger.debug("onSurfaceChanged. format:{}, width:{}, height:{}", format, width, height);
        mWidth = width;
        mHeight = height;
        mIsDrawing = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        logger.debug("onSurfaceDestroyed");
        mIsDrawing = false;
        mSurfaceHolder.removeCallback(this);
    }

    @Override
    public void run() {
        while (mIsDrawing) {
            logger.debug("draw canvas");
            mCanvas = mSurfaceHolder.lockCanvas();
            if (mCanvas != null) {
                try {
                    //使用获得的Canvas做具体的绘制
                    draw();
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.error(e);
                } finally {
                    mSurfaceHolder.unlockCanvasAndPost(mCanvas);
                }
            }
        }
    }

    private void draw() {
        mCanvas.drawColor(Color.WHITE);
        mCanvas.drawCircle(mWidth / 2, mHeight / 2, 200, mPaint);
    }
}
