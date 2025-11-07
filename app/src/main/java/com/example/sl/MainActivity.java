package com.example.sl;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private PointCloudGLSurfaceView glSurfaceView;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 显示加载对话框
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("正在加载点云数据...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 在后台线程加载点云
        new Thread(() -> {
            final PointCloudData pointCloudData = PCDParser.parsePCDFromAssets(MainActivity.this, "color_ASCII_a4_231114.pcd");

            runOnUiThread(() -> {
                progressDialog.dismiss();

                if (pointCloudData.pointCount == 0) {
                    Toast.makeText(MainActivity.this, "无法加载PCD文件或文件为空", Toast.LENGTH_LONG).show();
                    return;
                }

                glSurfaceView = new PointCloudGLSurfaceView(MainActivity.this, pointCloudData);
                setContentView(glSurfaceView);

//                Toast.makeText(MainActivity.this, "成功加载 " + pointCloudData.pointCount + " 个点", Toast.LENGTH_LONG).show();
            });
        }).start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (glSurfaceView != null) {
            glSurfaceView.onPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (glSurfaceView != null) {
            glSurfaceView.onResume();
        }
    }
}