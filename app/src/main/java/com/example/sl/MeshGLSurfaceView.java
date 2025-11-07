package com.example.sl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class MeshGLSurfaceView extends GLSurfaceView {
    private MeshRenderer renderer;
    private float previousX;
    private float previousY;
    private ScaleGestureDetector scaleDetector;
    private long lastTouchTime = 0;
    private static final int DOUBLE_TAP_TIME_DELTA = 300; // 毫秒

    public MeshGLSurfaceView(Context context, MeshRenderer renderer) {
        super(context);
        this.renderer = renderer;

        initialize();
    }

    public MeshGLSurfaceView(Context context, MeshData meshData) {
        super(context);
        this.renderer = new MeshRenderer(context, meshData);

        initialize();
    }

    private void initialize() {
        // 设置OpenGL ES 3.0
        setEGLContextClientVersion(3);

        // 设置渲染器
        setRenderer(renderer);

        // 设置渲染模式
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        // 或者使用连续渲染：setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // 初始化缩放手势检测器
        scaleDetector = new ScaleGestureDetector(getContext(), new ScaleListener());

        // 保持屏幕常亮
        setKeepScreenOn(true);

        Log.i("MeshGLSurfaceView", "MeshGLSurfaceView initialized");
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
//                        renderer.resetView();
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
                        requestRender();
                    }
                }
                previousX = x;
                previousY = y;
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 触摸结束处理
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 多指触摸开始
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // 多指触摸结束
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

    // 公开的方法用于外部控制

    public void setRenderer(MeshRenderer renderer) {
        this.renderer = renderer;
        super.setRenderer(renderer);
    }

    public MeshRenderer getMeshRenderer() {
        return renderer;
    }

    public void resetView() {
        if (renderer != null) {
//            renderer.resetView();
            requestRender();
        }
    }

    public void rotateView(float dx, float dy) {
        if (renderer != null) {
            renderer.rotate(dx, dy);
            requestRender();
        }
    }

    public void zoomView(float scale) {
        if (renderer != null) {
            renderer.zoom(scale);
            requestRender();
        }
    }

    // 切换渲染模式
    public void setContinuousRendering(boolean continuous) {
        if (continuous) {
            setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        } else {
            setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        }
    }
}
