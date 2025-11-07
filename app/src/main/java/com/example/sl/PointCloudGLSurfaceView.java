package com.example.sl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class PointCloudGLSurfaceView extends GLSurfaceView {
    private PointCloudRenderer renderer;
    private float previousX;
    private float previousY;
    private ScaleGestureDetector scaleDetector;
    private long lastTouchTime = 0;
    private static final int DOUBLE_TAP_TIME_DELTA = 300; // 毫秒

    public PointCloudGLSurfaceView(Context context, PointCloudData data, String mode) {
        super(context);

        setEGLContextClientVersion(3);
        renderer = new PointCloudRenderer(context, data, mode);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 初始化缩放手势检测器
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 先处理缩放手势
        scaleDetector.onTouchEvent(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // 双击检测
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastTouchTime < DOUBLE_TAP_TIME_DELTA) {
                    // 双击事件 - 重置视角
                    if (renderer != null) {
                        renderer.resetView();
                        requestRender();
                    }
                }
                lastTouchTime = currentTime;

                previousX = x;
                previousY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                // 如果没有在缩放，就处理旋转
                if (!scaleDetector.isInProgress() && event.getPointerCount() == 1) {
                    float dx = x - previousX;
                    float dy = y - previousY;
                    if (renderer != null) {
                        renderer.rotate(dx, dy);
                    }
                    requestRender();
                }
                previousX = x;
                previousY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            if (renderer != null) {
                float scaleFactor = detector.getScaleFactor();
                renderer.zoom(scaleFactor);
                requestRender();
                return true;
            }
            return false;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // 缩放结束处理
        }
    }

    public PointCloudRenderer getPointCloudRenderer() {
        return renderer;
    }
}
