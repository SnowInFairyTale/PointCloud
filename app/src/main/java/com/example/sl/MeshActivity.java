package com.example.sl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

// 在MainActivity中使用
public class MeshActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 转换为网格
        MeshData meshData = SurfaceReconstructor.poissonReconstruction(PointCloudDataHolder.getPointCloudData(), 0.1f);

        // 使用网格渲染器
        MeshRenderer renderer = new MeshRenderer(this, meshData);
        glSurfaceView = new MeshGLSurfaceView(this, renderer);

        setContentView(glSurfaceView);
    }
}
