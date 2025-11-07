package com.example.sl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

public class PointCloudGLSurfaceView extends GLSurfaceView {
    private PointCloudRenderer renderer;
    private float previousX;
    private float previousY;

    public PointCloudGLSurfaceView(Context context, PointCloudData data) {
        super(context);

        setEGLContextClientVersion(3);
        renderer = new PointCloudRenderer(context, data);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;
                renderer.rotate(dx, dy);
                requestRender();
                break;

            case MotionEvent.ACTION_DOWN:
                previousX = x;
                previousY = y;
                break;
        }

        previousX = x;
        previousY = y;
        return true;
    }
}
