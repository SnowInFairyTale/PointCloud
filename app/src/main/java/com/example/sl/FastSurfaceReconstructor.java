package com.example.sl;

import android.util.Log;

import java.util.*;

public class FastSurfaceReconstructor {
    private static final String TAG = "FastSurfaceReconstructor";

    /**
     * 快速表面重建 - 使用简化的贪婪投影三角化
     */
    public static MeshData fastReconstruction(PointCloudData pointCloud, float searchRadius) {
        Log.i(TAG, "Starting fast surface reconstruction...");
        long startTime = System.currentTimeMillis();

        // 第一步：数据预处理（降采样）
        PointCloudData sampledCloud = fastDownsample(pointCloud, 50000); // 限制到5万个点

        // 第二步：快速三角化
        MeshData meshData = greedyProjectionTriangulation(sampledCloud, searchRadius);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Fast reconstruction completed in %d ms: %d vertices, %d triangles", (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * 带目标点数的快速表面重建
     */
    public static MeshData fastReconstruction(PointCloudData pointCloud, float searchRadius, int targetPoints) {
        Log.i(TAG, "Starting fast surface reconstruction with target points...");
        long startTime = System.currentTimeMillis();

        // 第一步：数据预处理（降采样）
        PointCloudData sampledCloud = fastDownsample(pointCloud, targetPoints);

        // 第二步：快速三角化
        MeshData meshData = greedyProjectionTriangulation(sampledCloud, searchRadius);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Fast reconstruction completed in %d ms: %d vertices, %d triangles", (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * 快速降采样 - 使用体素网格滤波
     */
    private static PointCloudData fastDownsample(PointCloudData pointCloud, int targetPoints) {
        if (pointCloud.pointCount <= targetPoints) {
            return pointCloud;
        }

        Log.i(TAG, "Downsampling from " + pointCloud.pointCount + " to " + targetPoints + " points");

        // 计算合适的体素大小
        float volume = (pointCloud.maxX - pointCloud.minX) * (pointCloud.maxY - pointCloud.minY) * (pointCloud.maxZ - pointCloud.minZ);
        float voxelSize = (float) Math.pow(volume / targetPoints, 1.0 / 3.0);

        // 体素网格滤波
        Map<String, float[]> voxelMap = new HashMap<>();
        Map<String, float[]> colorMap = new HashMap<>();

        for (int i = 0; i < pointCloud.points.size(); i++) {
            float[] point = pointCloud.points.get(i);
            float[] color = pointCloud.colors.get(i);

            // 计算体素坐标
            int voxelX = (int) (point[0] / voxelSize);
            int voxelY = (int) (point[1] / voxelSize);
            int voxelZ = (int) (point[2] / voxelSize);
            String voxelKey = voxelX + "," + voxelY + "," + voxelZ;

            // 每个体素只保留一个点（第一个点）
            if (!voxelMap.containsKey(voxelKey)) {
                voxelMap.put(voxelKey, point);
                colorMap.put(voxelKey, color);
            }
        }

        // 创建降采样后的点云
        PointCloudData sampled = new PointCloudData();
        for (String key : voxelMap.keySet()) {
            float[] point = voxelMap.get(key);
            float[] color = colorMap.get(key);
            sampled.addPoint(point[0], point[1], point[2], color[0], color[1], color[2]);
        }

        Log.i(TAG, "Downsampling completed: " + sampled.pointCount + " points");
        return sampled;
    }

    /**
     * 贪婪投影三角化 - 简化的快速算法
     */
    private static MeshData greedyProjectionTriangulation(PointCloudData pointCloud, float searchRadius) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        // 将点云直接作为顶点
        vertices.addAll(pointCloud.points);

        // 为每个点计算法线（简化版本）
        for (int i = 0; i < pointCloud.points.size(); i++) {
            float[] normal = estimateNormal(pointCloud, i, searchRadius);
            normals.add(normal);
        }

        // 简化的三角化 - 基于空间邻近性
        buildTrianglesFromNeighbors(pointCloud, vertices, triangles, searchRadius);

        return new MeshData(vertices, normals, triangles);
    }

    /**
     * 估计法线 - 使用PCA分析邻近点
     */
    private static float[] estimateNormal(PointCloudData pointCloud, int pointIndex, float radius) {
        float[] point = pointCloud.points.get(pointIndex);
        List<float[]> neighbors = findNeighbors(pointCloud, point, radius);

        if (neighbors.size() < 3) {
            return new float[]{0, 1, 0}; // 默认法线
        }

        // 计算质心
        float[] centroid = new float[3];
        for (float[] neighbor : neighbors) {
            centroid[0] += neighbor[0];
            centroid[1] += neighbor[1];
            centroid[2] += neighbor[2];
        }
        centroid[0] /= neighbors.size();
        centroid[1] /= neighbors.size();
        centroid[2] /= neighbors.size();

        // 简化版本 - 返回近似法线
        // 实际应该使用完整的PCA计算最小特征值对应的特征向量
        return new float[]{0, 1, 0};
    }

    /**
     * 基于邻近点构建三角形
     */
    private static void buildTrianglesFromNeighbors(PointCloudData pointCloud, List<float[]> vertices, List<int[]> triangles, float radius) {
        int maxTriangles = Math.min(100000, vertices.size() * 2); // 限制三角形数量

        // 使用KD树或空间索引加速邻近搜索（这里简化实现）
        for (int i = 0; i < vertices.size() && triangles.size() < maxTriangles; i++) {
            float[] point = vertices.get(i);
            List<Integer> neighborIndices = findNeighborIndices(pointCloud, point, radius);

            // 为每个邻近点对创建三角形
            for (int j = 0; j < neighborIndices.size() && triangles.size() < maxTriangles; j++) {
                for (int k = j + 1; k < neighborIndices.size() && triangles.size() < maxTriangles; k++) {
                    int idx1 = neighborIndices.get(j);
                    int idx2 = neighborIndices.get(k);

                    // 检查三角形是否有效（边长合理）
                    if (isValidTriangle(point, vertices.get(idx1), vertices.get(idx2), radius * 2)) {
                        triangles.add(new int[]{i, idx1, idx2});
                    }
                }
            }
        }

        Log.i(TAG, "Generated " + triangles.size() + " triangles");
    }

    /**
     * 查找邻近点
     */
    private static List<float[]> findNeighbors(PointCloudData pointCloud, float[] point, float radius) {
        List<float[]> neighbors = new ArrayList<>();
        float radiusSq = radius * radius;

        for (float[] other : pointCloud.points) {
            float dx = other[0] - point[0];
            float dy = other[1] - point[1];
            float dz = other[2] - point[2];
            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 0 && distSq < radiusSq) {
                neighbors.add(other);
            }
        }

        return neighbors;
    }

    private static List<Integer> findNeighborIndices(PointCloudData pointCloud, float[] point, float radius) {
        List<Integer> neighbors = new ArrayList<>();
        float radiusSq = radius * radius;

        for (int i = 0; i < pointCloud.points.size(); i++) {
            float[] other = pointCloud.points.get(i);
            float dx = other[0] - point[0];
            float dy = other[1] - point[1];
            float dz = other[2] - point[2];
            float distSq = dx * dx + dy * dy + dz * dz;

            if (distSq > 0 && distSq < radiusSq) {
                neighbors.add(i);
            }
        }

        return neighbors;
    }

    /**
     * 检查三角形是否有效
     */
    private static boolean isValidTriangle(float[] p1, float[] p2, float[] p3, float maxEdgeLength) {
        float maxEdgeSq = maxEdgeLength * maxEdgeLength;

        float d12 = distanceSq(p1, p2);
        float d23 = distanceSq(p2, p3);
        float d31 = distanceSq(p3, p1);

        return d12 < maxEdgeSq && d23 < maxEdgeSq && d31 < maxEdgeSq;
    }

    private static float distanceSq(float[] p1, float[] p2) {
        float dx = p1[0] - p2[0];
        float dy = p1[1] - p2[1];
        float dz = p1[2] - p2[2];
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * 超快速重建 - 使用最简化的算法
     */
    public static MeshData ultraFastReconstruction(PointCloudData pointCloud) {
        Log.i(TAG, "Starting ultra fast surface reconstruction...");
        long startTime = System.currentTimeMillis();

        // 极速降采样
        PointCloudData sampledCloud = ultraFastDownsample(pointCloud, 10000);

        // 极速三角化
        MeshData meshData = ultraFastTriangulation(sampledCloud);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Ultra fast reconstruction completed in %d ms: %d vertices, %d triangles", (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * 极速降采样 - 均匀采样
     */
    private static PointCloudData ultraFastDownsample(PointCloudData pointCloud, int targetPoints) {
        if (pointCloud.pointCount <= targetPoints) {
            return pointCloud;
        }

        PointCloudData sampled = new PointCloudData();
        int step = pointCloud.pointCount / targetPoints;

        for (int i = 0; i < pointCloud.points.size() && sampled.pointCount < targetPoints; i += step) {
            float[] point = pointCloud.points.get(i);
            float[] color = pointCloud.colors.get(i);
            sampled.addPoint(point[0], point[1], point[2], color[0], color[1], color[2]);
        }

        return sampled;
    }

    /**
     * 极速三角化 - 直接连接相邻点
     */
    private static MeshData ultraFastTriangulation(PointCloudData pointCloud) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        vertices.addAll(pointCloud.points);

        // 所有点使用默认法线
        for (int i = 0; i < pointCloud.points.size(); i++) {
            normals.add(new float[]{0, 1, 0});
        }

        // 极简三角化：每三个点组成一个三角形
        for (int i = 0; i < pointCloud.points.size() - 2; i += 3) {
            triangles.add(new int[]{i, i + 1, i + 2});
        }

        return new MeshData(vertices, normals, triangles);
    }
}
