package com.example.sl;

import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

// 在MainActivity中使用
public class MeshActivity extends AppCompatActivity {
    private GLSurfaceView glSurfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("cdscsdcds", System.currentTimeMillis() + "");
        // 转换为网格
        MeshData meshData = FastSurfaceReconstructor.fastReconstruction(PointCloudDataHolder.getPointCloudData(), 0.1f, 100000);

        // 使用网格渲染器
        MeshRenderer renderer = new MeshRenderer(this, meshData);
        glSurfaceView = new MeshGLSurfaceView(this, renderer);
        Log.e("cdscsdcds", System.currentTimeMillis() + "  dd");

        setContentView(glSurfaceView);
    }
}
