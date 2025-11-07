package com.example.sl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MeshRenderer implements GLSurfaceView.Renderer {
    private static final String TAG = "MeshRenderer";

    private Context context;
    private MeshData meshData;

    private int program;
    private int positionHandle;
    private int normalHandle;
    private int texCoordHandle;
    private int mvpMatrixHandle;
    private int modelMatrixHandle;
    private int normalMatrixHandle;
    private int textureHandle;
    private int lightPositionHandle;

    private final float[] mvpMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] modelMatrix = new float[16];
    private final float[] normalMatrix = new float[16];

    private int vao;
    private int[] vbo = new int[4]; // 顶点, 法线, 纹理坐标, 索引
    private int textureId;

    private float rotationX = 0.0f;
    private float rotationY = 0.0f;
    private float distance = 5.0f;

    public MeshRenderer(Context context, MeshData meshData) {
        this.context = context;
        this.meshData = meshData;

        Matrix.setIdentityM(modelMatrix, 0);
        Matrix.setIdentityM(viewMatrix, 0);
        Matrix.setIdentityM(projectionMatrix, 0);
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i(TAG, "MeshRenderer onSurfaceCreated");

        // 设置背景色
        GLES30.glClearColor(0.2f, 0.3f, 0.4f, 1.0f);

        // 启用深度测试
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);

        // 启用背面剔除
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_BACK);

        // 加载着色器
        loadShaders();

        // 加载纹理
        loadTexture();

        // 设置缓冲区
        setupBuffers();
    }

    private void loadShaders() {
        String vertexShaderCode = ShaderUtils.readShaderFromAssets(context, "shader/mesh/vertex_shader.glsl");
        String fragmentShaderCode = ShaderUtils.readShaderFromAssets(context, "shader/mesh/fragment_shader.glsl");

        program = ShaderUtils.createProgram(vertexShaderCode, fragmentShaderCode);

        if (program == 0) {
            Log.e(TAG, "Failed to create mesh shader program");
            return;
        }

        // 获取attribute和uniform位置
        positionHandle = GLES30.glGetAttribLocation(program, "aPosition");
        normalHandle = GLES30.glGetAttribLocation(program, "aNormal");
        texCoordHandle = GLES30.glGetAttribLocation(program, "aTexCoord");
        mvpMatrixHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix");
        modelMatrixHandle = GLES30.glGetUniformLocation(program, "uModelMatrix");
        normalMatrixHandle = GLES30.glGetUniformLocation(program, "uNormalMatrix");
        textureHandle = GLES30.glGetUniformLocation(program, "uTexture");
        lightPositionHandle = GLES30.glGetUniformLocation(program, "uLightPosition");

        Log.i(TAG, "Mesh shader attributes loaded");
    }

    private void loadTexture() {
        int[] textures = new int[1];
        GLES30.glGenTextures(1, textures, 0);
        textureId = textures[0];

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);

        // 设置纹理参数
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);

        // 加载纹理图片
        try {
            InputStream is = context.getAssets().open("textures/brick.jpg");
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bitmap, 0);
            GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);

            bitmap.recycle();
            is.close();

            Log.i(TAG, "Texture loaded successfully");
        } catch (IOException e) {
            Log.e(TAG, "Failed to load texture: " + e.getMessage());

            // 创建默认纹理
            createDefaultTexture();
        }

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
    }

    private void createDefaultTexture() {
        // 创建棋盘格纹理
        int size = 64;
        ByteBuffer buffer = ByteBuffer.allocateDirect(size * size * 4);
        buffer.order(ByteOrder.nativeOrder());

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean isWhite = ((x / 8) + (y / 8)) % 2 == 0;
                byte color = isWhite ? (byte) 255 : (byte) 128;

                buffer.put(color); // R
                buffer.put(color); // G
                buffer.put(color); // B
                buffer.put((byte) 255); // A
            }
        }
        buffer.position(0);

        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, size, size, 0,
                GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer);
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
    }

    private void setupBuffers() {
        if (meshData == null) return;

        // 生成VAO
        int[] vaoArray = new int[1];
        GLES30.glGenVertexArrays(1, vaoArray, 0);
        vao = vaoArray[0];

        // 生成VBOs
        GLES30.glGenBuffers(4, vbo, 0);

        // 绑定VAO
        GLES30.glBindVertexArray(vao);

        // 顶点数据
        float[] vertices = meshData.getVerticesArray();
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertexBuffer = FloatBuffer.wrap(vertices);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.length * 4, vertexBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(positionHandle);
        GLES30.glVertexAttribPointer(positionHandle, 3, GLES30.GL_FLOAT, false, 12, 0);

        // 法线数据
        float[] normals = meshData.getNormalsArray();
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer normalBuffer = FloatBuffer.wrap(normals);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, normals.length * 4, normalBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(normalHandle);
        GLES30.glVertexAttribPointer(normalHandle, 3, GLES30.GL_FLOAT, false, 12, 0);

        // 纹理坐标
        float[] texCoords = meshData.getTexCoordsArray();
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer texCoordBuffer = FloatBuffer.wrap(texCoords);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, texCoords.length * 4, texCoordBuffer, GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(texCoordHandle);
        GLES30.glVertexAttribPointer(texCoordHandle, 2, GLES30.GL_FLOAT, false, 8, 0);

        // 索引数据
        int[] indices = meshData.getIndicesArray();
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
        IntBuffer indexBuffer = IntBuffer.wrap(indices);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.length * 4, indexBuffer, GLES30.GL_STATIC_DRAW);

        // 解绑
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0);
        GLES30.glBindVertexArray(0);

        Log.i(TAG, "Mesh buffers setup completed");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        float ratio = (float) width / height;
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 1, 100);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);

        if (meshData == null || program == 0) return;

        // 更新相机
        updateCamera();

        // 计算法线矩阵
        Matrix.invertM(normalMatrix, 0, modelMatrix, 0);
        Matrix.transposeM(normalMatrix, 0, normalMatrix, 0);

        // 使用着色器
        GLES30.glUseProgram(program);

        // 设置矩阵
        GLES30.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES30.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(normalMatrixHandle, 1, false, normalMatrix, 0);

        // 设置光源位置
        GLES30.glUniform3f(lightPositionHandle, 2.0f, 5.0f, 3.0f);

        // 绑定纹理
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId);
        GLES30.glUniform1i(textureHandle, 0);

        // 绘制网格
        GLES30.glBindVertexArray(vao);
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, meshData.getIndicesArray().length, GLES30.GL_UNSIGNED_INT, 0);
        GLES30.glBindVertexArray(0);
    }

    private void updateCamera() {
        float eyeX = (float) (distance * Math.sin(Math.toRadians(rotationY)) * Math.cos(Math.toRadians(rotationX)));
        float eyeY = (float) (distance * Math.sin(Math.toRadians(rotationX)));
        float eyeZ = (float) (distance * Math.cos(Math.toRadians(rotationY)) * Math.cos(Math.toRadians(rotationX)));

        Matrix.setLookAtM(viewMatrix, 0,
                eyeX, eyeY, eyeZ,
                0, 0, 0,
                0, 1, 0
        );

        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
    }

    public void rotate(float dx, float dy) {
        rotationY += dx * 0.5f;
        rotationX += dy * 0.5f;
    }

    public void zoom(float scale) {
        distance *= (1.0f / scale);
        if (distance < 2.0f) distance = 2.0f;
        if (distance > 20.0f) distance = 20.0f;
    }
}
