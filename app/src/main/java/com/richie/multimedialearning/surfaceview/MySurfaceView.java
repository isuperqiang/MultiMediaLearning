package com.richie.multimedialearning.surfaceview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.richie.easylog.ILogger;
import com.richie.easylog.LoggerFactory;
import com.richie.multimedialearning.utils.BitmapUtils;

import java.io.File;

/**
 * @author Richie on 2018.10.22
 */
public class MySurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {
    private final ILogger logger = LoggerFactory.getLogger(MySurfaceView.class);
    private volatile boolean mIsDrawing;
    private SurfaceHolder mSurfaceHolder;
    private Paint mPaint;
    private Thread mThread;
    private Bitmap mBitmap;

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
        mPaint.setStyle(Paint.Style.STROKE);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        logger.info("onSurfaceCreated");
        mThread = new Thread(this, "Surface-Renderer");
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        logger.info("onSurfaceChanged. format:{}, width:{}, height:{}", format, width, height);
        mBitmap = BitmapUtils.decodeSampledBitmapFromFile(new File(getContext().getExternalFilesDir(null),
                "template.jpg"), width, height);
        mIsDrawing = true;
        mThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        logger.info("onSurfaceDestroyed");
        mIsDrawing = false;
        mThread.interrupt();
    }

    @Override
    public void run() {
        while (mIsDrawing) {
            logger.debug("draw canvas");
            Canvas canvas = mSurfaceHolder.lockCanvas();
            if (canvas != null) {
                try {
                    //使用获得的Canvas做具体的绘制
                    drawCanvas(canvas);
                    // 睡眠 100ms
                    Thread.sleep(100);
                } catch (Exception e) {
                    logger.error(e);
                } finally {
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    private void drawCanvas(Canvas canvas) {
        canvas.drawBitmap(mBitmap, 0, 0, mPaint);
    }
}
