package com.example.sl;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class PointCloudData {
    public List<float[]> points;
    public List<float[]> colors;
    public int pointCount;

    // 用于统计点的范围
    public float minX = Float.MAX_VALUE, maxX = Float.MIN_VALUE;
    public float minY = Float.MAX_VALUE, maxY = Float.MIN_VALUE;
    public float minZ = Float.MAX_VALUE, maxZ = Float.MIN_VALUE;

    public PointCloudData() {
        points = new ArrayList<>();
        colors = new ArrayList<>();
        pointCount = 0;
    }

    public void addPoint(float x, float y, float z) {
        points.add(new float[]{x, y, z});
        updateBounds(x, y, z);

        // 基于高度生成颜色
        float normalizedY = (y - minY) / (maxY - minY + 0.001f);
        float r = normalizedY;
        float g = 0.5f;
        float b = 1.0f - normalizedY;
        colors.add(new float[]{r, g, b, 1.0f});

        pointCount++;
    }

    public void addPoint(float x, float y, float z, float r, float g, float b) {
        points.add(new float[]{x, y, z});
        updateBounds(x, y, z);
        colors.add(new float[]{r, g, b, 1.0f});
        pointCount++;
    }

    private void updateBounds(float x, float y, float z) {
        if (x < minX) minX = x;
        if (x > maxX) maxX = x;
        if (y < minY) minY = y;
        if (y > maxY) maxY = y;
        if (z < minZ) minZ = z;
        if (z > maxZ) maxZ = z;
    }

    public float[] getPointsArray() {
        float[] array = new float[pointCount * 3];
        for (int i = 0; i < pointCount; i++) {
            float[] point = points.get(i);
            array[i * 3] = point[0];
            array[i * 3 + 1] = point[1];
            array[i * 3 + 2] = point[2];
        }
        return array;
    }

    public float[] getColorsArray() {
        float[] array = new float[pointCount * 4];
        for (int i = 0; i < pointCount; i++) {
            float[] color = colors.get(i);
            array[i * 4] = color[0];
            array[i * 4 + 1] = color[1];
            array[i * 4 + 2] = color[2];
            array[i * 4 + 3] = color[3];
        }
        return array;
    }

    public void normalizePoints() {
        // 将点云数据归一化到 [-1, 1] 范围
        float centerX = (minX + maxX) / 2.0f;
        float centerY = (minY + maxY) / 2.0f;
        float centerZ = (minZ + maxZ) / 2.0f;
        float scale = Math.max(maxX - minX, Math.max(maxY - minY, maxZ - minZ)) / 2.0f;

        if (scale < 0.001f) scale = 1.0f;

        for (int i = 0; i < points.size(); i++) {
            float[] point = points.get(i);
            point[0] = (point[0] - centerX) / scale;
            point[1] = (point[1] - centerY) / scale;
            point[2] = (point[2] - centerZ) / scale;
        }

        // 更新边界
        minX = -1.0f; maxX = 1.0f;
        minY = -1.0f; maxY = 1.0f;
        minZ = -1.0f; maxZ = 1.0f;
    }

    public void logBounds() {
        Log.i("PointCloudData", String.format(
                "Point bounds: X[%.2f, %.2f] Y[%.2f, %.2f] Z[%.2f, %.2f]",
                minX, maxX, minY, maxY, minZ, maxZ));
    }
}
