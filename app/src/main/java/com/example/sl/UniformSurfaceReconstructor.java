package com.example.sl;

import android.util.Log;

public class UniformSurfaceReconstructor {
    private static final String TAG = "UniformSurfaceReconstructor";

    /**
     * 均匀取样 + 表面重建
     */
    public static MeshData uniformReconstruct(PointCloudData pointCloud, int targetPoints) {
        Log.i(TAG, "Starting uniform surface reconstruction...");
        long startTime = System.currentTimeMillis();

        // 1. 均匀取样
        PointCloudData sampled = UniformSampler.uniformSample(pointCloud, targetPoints);

        // 2. 表面重建
        MeshData meshData = QuickSurfaceReconstructor.quickReconstruct(sampled, sampled.pointCount);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Uniform reconstruction completed in %d ms: %d vertices, %d triangles", (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * 自动选择目标点数的重建
     */
    public static MeshData autoReconstruct(PointCloudData pointCloud) {
        // 根据原始点数自动选择目标点数
        int targetPoints;
        if (pointCloud.pointCount > 1000000) {
            targetPoints = 5000;  // 100万+点 → 5万点
        } else if (pointCloud.pointCount > 100000) {
            targetPoints = 3000;  // 10万-100万点 → 3万点
        } else if (pointCloud.pointCount > 10000) {
            targetPoints = 1000;  // 1万-10万点 → 1万点
        } else {
            targetPoints = pointCloud.pointCount; // 少于1万点，使用全部
        }

        return uniformReconstruct(pointCloud, targetPoints);
    }
}
