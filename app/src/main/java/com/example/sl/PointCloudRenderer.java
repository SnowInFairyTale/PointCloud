package com.example.sl;

import android.content.Context;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.FloatBuffer;

public class PointCloudRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "PointCloudRenderer";

    private Context context;
    private PointCloudData pointCloudData;

    private int program;
    private int positionHandle;
    private int colorHandle;
    private int mvpMatrixHandle;

    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];

    private int[] vbo = new int[2]; // 0: positions, 1: colors
    private int vao;

    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float distance = 3.0f;
    private float[] centerPoint = new float[3]; // 点云中心点

    // 触摸控制相关
    private float previousX;
    private float previousY;
    private boolean isRotating = false;

    public PointCloudRenderer(Context context, PointCloudData data) {
        this.context = context;
        this.pointCloudData = data;

        // 初始化矩阵
        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setIdentityM(projectionMatrix, 0);

        // 归一化点云数据以便更好地显示
        if (pointCloudData != null) {
            pointCloudData.normalizePoints();
            pointCloudData.logBounds();

            // 计算中心点（归一化后应该是原点附近）
            centerPoint[0] = (pointCloudData.minX + pointCloudData.maxX) / 2.0f;
            centerPoint[1] = (pointCloudData.minY + pointCloudData.maxY) / 2.0f;
            centerPoint[2] = (pointCloudData.minZ + pointCloudData.maxZ) / 2.0f;
        }
    }

    public void rotate(float dx, float dy) {
        rotationY += dx * 0.5f;
        rotationX += dy * 0.5f;

        // 限制X轴旋转角度
        if (rotationX > 90.0f) rotationX = 90.0f;
        if (rotationX < -90.0f) rotationX = -90.0f;
    }

    public void zoom(float scale) {
        distance *= scale;
        if (distance < 1.0f) distance = 1.0f;
        if (distance > 20.0f) distance = 20.0f;
    }

    public void resetView() {
        rotationX = 0.0f;
        rotationY = 0.0f;
        distance = 3.0f;
    }

    public void setPointCloudData(PointCloudData data) {
        this.pointCloudData = data;
        if (pointCloudData != null) {
            pointCloudData.normalizePoints();
            pointCloudData.logBounds();

            // 重新计算中心点
            centerPoint[0] = (pointCloudData.minX + pointCloudData.maxX) / 2.0f;
            centerPoint[1] = (pointCloudData.minY + pointCloudData.maxY) / 2.0f;
            centerPoint[2] = (pointCloudData.minZ + pointCloudData.maxZ) / 2.0f;

            // 重新设置缓冲区
            if (program != 0) {
                setupBuffers();
            }
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");

        // 设置深灰色背景
        GLES30.glClearColor(0.1f, 0.1f, 0.1f, 1.0f);

        // 启用深度测试
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);

        // 启用点平滑（可选）
        GLES30.glEnable(GLES30.GL_POINT_SMOOTH);
        GLES30.glHint(GLES30.GL_POINT_SMOOTH_HINT, GLES30.GL_NICEST);

        // 混合设置（如果使用透明效果）
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);

        // 加载和创建着色器程序
        String vertexShaderCode = ShaderUtils.readShaderFromAssets(context, "vertex_shader.glsl");
        String fragmentShaderCode = ShaderUtils.readShaderFromAssets(context, "fragment_shader.glsl");

        if (vertexShaderCode == null || fragmentShaderCode == null) {
            Log.e(TAG, "Failed to load shader code");
            return;
        }

        program = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        if (program == 0) {
            Log.e(TAG, "Failed to create shader program");
            return;
        }

        // 获取uniform和attribute位置
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition");
        colorHandle = GLES30.glGetAttribLocation(program, "aColor");
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");

        Log.i(TAG, "Shader attributes - position: " + positionHandle +
                ", color: " + colorHandle + ", mvp: " + mvpMatrixHandle);

        // 设置缓冲区
        setupBuffers();

        // 检查OpenGL错误
        checkGLError("onSurfaceCreated");
    }

    private void setupBuffers() {
        if (pointCloudData == null || pointCloudData.pointCount == 0) {
            Log.e(TAG, "No point cloud data available for buffer setup");
            return;
        }

        try {
            Log.i(TAG, "Setting up buffers for " + pointCloudData.pointCount + " points");

            // 删除旧的缓冲区（如果存在）
            cleanupBuffers();

            // 生成VAO
            int[] vaoArray = new int[1];
            GLES30.glGenVertexArrays(1, vaoArray, 0);
            vao = vaoArray[0];

            // 生成VBOs (2个：位置和颜色)
            vbo = new int[2];
            GLES30.glGenBuffers(2, vbo, 0);

            // 绑定VAO
            GLES30.glBindVertexArray(vao);

            // 设置位置VBO
            float[] positions = pointCloudData.getPointsArray();
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
            FloatBuffer positionBuffer = FloatBuffer.wrap(positions);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, positions.length * 4,
                    positionBuffer, GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(positionHandle);
            GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, 0);

            // 设置颜色VBO
            float[] colors = pointCloudData.getColorsArray();
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[1]);
            FloatBuffer colorBuffer = FloatBuffer.wrap(colors);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, colors.length * 4,
                    colorBuffer, GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(colorHandle);
            GLES30.glVertexAttribPointer(colorHandle, 4, GLES30.GL_FLOAT, false, 16, 0);

            // 解绑
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
            GLES30.glBindVertexArray(0);

            Log.i(TAG, "Buffers created successfully - VAO: " + vao +
                    ", VBOs: " + vbo[0] + ", " + vbo[1]);

            // 检查OpenGL错误
            checkGLError("setupBuffers");

        } catch (Exception e) {
            Log.e(TAG, "Error setting up buffers: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i(TAG, "onSurfaceChanged: " + width + "x" + height);

        GLES30.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // 设置透视投影
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);

        // 或者使用透视投影（可选）
        // Matrix.perspectiveM(projectionMatrix, 0, 45.0f, ratio, 0.1f, 100.0f);

        checkGLError("onSurfaceChanged");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // 清除颜色和深度缓冲
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (pointCloudData == null || pointCloudData.pointCount == 0 || program == 0) {
            return;
        }

        // 设置相机位置 - 围绕点云中心旋转
        float eyeX = (float) (distance * Math.sin(Math.toRadians(rotationY)) * Math.cos(Math.toRadians(rotationX)));
        float eyeY = (float) (distance * Math.sin(Math.toRadians(rotationX)));
        float eyeZ = (float) (distance * Math.cos(Math.toRadians(rotationY)) * Math.cos(Math.toRadians(rotationX)));

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,           // 眼睛位置
                centerPoint[0], centerPoint[1], centerPoint[2], // 观察点（点云中心）
                0, 1, 0                     // 上向量
        );

        // 应用模型变换（目前主要是旋转）
        Matrix.setIdentityM(modelMatrix, 0);

        // 计算MVP矩阵: Projection * View * Model
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);

        // 使用着色器程序
        GLES30.glUseProgram(program);

        // 设置MVP矩阵
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);

        // 绘制点
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawArrays(GLES30.GL_POINTS, 0, pointCloudData.pointCount);
        GLES30.glBindVertexArray(0);

        // 检查OpenGL错误
        int error = GLES30.glGetError();
        if (error != GLES30.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error in onDrawFrame: " + error);
        }
    }

    private void checkGLError(String operation) {
        int error;
        while ((error = GLES30.glGetError()) != GLES30.GL_NO_ERROR) {
            Log.e(TAG, operation + ": OpenGL error: 0x" + Integer.toHexString(error));
        }
    }

    private void cleanupBuffers() {
        if (vao != 0) {
            int[] vaoArray = {vao};
            GLES30.glDeleteVertexArrays(1, vaoArray, 0);
            vao = 0;
        }
        if (vbo != null) {
            if (vbo[0] != 0 || vbo[1] != 0) {
                GLES30.glDeleteBuffers(2, vbo, 0);
                vbo[0] = 0;
                vbo[1] = 0;
            }
        }
    }

    public void cleanup() {
        Log.i(TAG, "Cleaning up OpenGL resources");

        cleanupBuffers();

        if (program != 0) {
            GLES30.glDeleteProgram(program);
            program = 0;
        }
    }

    // 获取当前渲染状态信息
    public String getRenderInfo() {
        if (pointCloudData == null) {
            return "No point cloud data";
        }

        return String.format(
                "Points: %,d | Distance: %.1f | Rotation: (%.1f, %.1f)",
                pointCloudData.pointCount, distance, rotationX, rotationY
        );
    }

    // 处理触摸事件的方法
    public void handleTouchEvent(int action, float x, float y) {
        switch (action) {
            case android.view.MotionEvent.ACTION_DOWN:
                previousX = x;
                previousY = y;
                isRotating = true;
                break;

            case android.view.MotionEvent.ACTION_MOVE:
                if (isRotating) {
                    float dx = x - previousX;
                    float dy = y - previousY;
                    rotate(dx, dy);
                    previousX = x;
                    previousY = y;
                }
                break;

            case android.view.MotionEvent.ACTION_UP:
            case android.view.MotionEvent.ACTION_CANCEL:
                isRotating = false;
                break;
        }
    }

    // 处理缩放事件
    public void handleScaleEvent(float scaleFactor) {
        zoom(scaleFactor);
    }
}
