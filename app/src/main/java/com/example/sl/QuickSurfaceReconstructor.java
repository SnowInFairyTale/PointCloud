package com.example.sl;

import android.util.Log;
import java.util.*;

public class QuickSurfaceReconstructor {
    private static final String TAG = "QuickSurfaceReconstructor";

    /**
     * 快速表面重建 - 直接对点云进行三角化
     */
    public static MeshData quickReconstruct(PointCloudData pointCloud, int targetPoints) {
        Log.i(TAG, "Starting quick surface reconstruction...");
        long startTime = System.currentTimeMillis();

        // 1. 降采样到合适数量
        PointCloudData sampled = fastDownsample(pointCloud, targetPoints);

        // 2. 快速三角化
        MeshData meshData = quickTriangulation(sampled);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Quick reconstruction completed in %d ms: %d vertices, %d triangles",
                (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * 快速降采样
     */
    private static PointCloudData fastDownsample(PointCloudData pointCloud, int targetPoints) {
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

        Log.i(TAG, "Downsampled from " + pointCloud.pointCount + " to " + sampled.pointCount + " points");
        return sampled;
    }

    /**
     * 快速三角化 - 使用Delaunay-like算法
     */
    private static MeshData quickTriangulation(PointCloudData pointCloud) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        // 直接使用点云作为顶点
        vertices.addAll(pointCloud.points);

        // 计算法线（简化版本）
        for (int i = 0; i < vertices.size(); i++) {
            normals.add(calculateSimpleNormal(pointCloud, i));
        }

        // 生成三角形网格
        generateTriangleMesh(pointCloud, vertices, triangles);

        return new MeshData(vertices, normals, triangles);
    }

    /**
     * 简化法线计算
     */
    private static float[] calculateSimpleNormal(PointCloudData pointCloud, int index) {
        // 找到最近的几个点计算法线
        float[] point = pointCloud.points.get(index);
        List<Integer> neighbors = findClosestNeighbors(pointCloud, point, 10); // 找10个最近点

        if (neighbors.size() < 3) {
            return new float[]{0, 1, 0}; // 默认法线
        }

        // 使用PCA计算法线（简化版）
        return computePCANormal(pointCloud, point, neighbors);
    }

    /**
     * 生成三角形网格
     */
    private static void generateTriangleMesh(PointCloudData pointCloud,
                                             List<float[]> vertices,
                                             List<int[]> triangles) {
        int pointCount = vertices.size();
        int maxTriangles = Math.min(100000, pointCount * 2);

        // 简化的网格生成：连接邻近点形成三角形
        for (int i = 0; i < pointCount - 2 && triangles.size() < maxTriangles; i++) {
            float[] p1 = vertices.get(i);

            // 找到两个最近的点
            int closest1 = -1, closest2 = -1;
            float dist1 = Float.MAX_VALUE, dist2 = Float.MAX_VALUE;

            for (int j = i + 1; j < pointCount; j++) {
                if (j == i) continue;

                float[] p2 = vertices.get(j);
                float dist = distance(p1, p2);

                if (dist < dist1) {
                    dist2 = dist1;
                    closest2 = closest1;
                    dist1 = dist;
                    closest1 = j;
                } else if (dist < dist2) {
                    dist2 = dist;
                    closest2 = j;
                }
            }

            if (closest1 != -1 && closest2 != -1) {
                // 检查三角形是否合理
                if (isValidTriangle(p1, vertices.get(closest1), vertices.get(closest2))) {
                    triangles.add(new int[]{i, closest1, closest2});
                }
            }
        }

        // 如果三角形太少，使用更简单的方法
        if (triangles.size() < pointCount / 10) {
            generateSimpleTriangles(vertices, triangles, maxTriangles);
        }

        Log.i(TAG, "Generated " + triangles.size() + " triangles from " + pointCount + " points");
    }

    /**
     * 简单三角形生成：每三个连续点组成三角形
     */
    private static void generateSimpleTriangles(List<float[]> vertices,
                                                List<int[]> triangles,
                                                int maxTriangles) {
        Log.i(TAG, "Using simple triangle generation");
        for (int i = 0; i < vertices.size() - 2 && triangles.size() < maxTriangles; i += 1) {
            triangles.add(new int[]{i, i + 1, i + 2});
        }
    }

    /**
     * 查找最近邻居
     */
    private static List<Integer> findClosestNeighbors(PointCloudData pointCloud, float[] point, int k) {
        List<Neighbor> neighbors = new ArrayList<>();

        for (int i = 0; i < pointCloud.points.size(); i++) {
            float[] other = pointCloud.points.get(i);
            float dist = distance(point, other);

            if (dist > 0.001f) { // 排除自身
                neighbors.add(new Neighbor(i, dist));
            }
        }

        // 按距离排序
        Collections.sort(neighbors, (a, b) -> Float.compare(a.distance, b.distance));

        // 返回前k个
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < Math.min(k, neighbors.size()); i++) {
            result.add(neighbors.get(i).index);
        }

        return result;
    }

    /**
     * PCA法线计算
     */
    private static float[] computePCANormal(PointCloudData pointCloud, float[] point, List<Integer> neighbors) {
        // 计算质心
        float[] centroid = new float[3];
        for (int idx : neighbors) {
            float[] p = pointCloud.points.get(idx);
            centroid[0] += p[0];
            centroid[1] += p[1];
            centroid[2] += p[2];
        }
        centroid[0] /= neighbors.size();
        centroid[1] /= neighbors.size();
        centroid[2] /= neighbors.size();

        // 构建协方差矩阵（简化实现）
        // 实际应该计算完整的3x3协方差矩阵和特征值分解
        // 这里返回一个近似的法线

        // 使用质心到原点的方向作为法线（简化）
        float dx = centroid[0] - point[0];
        float dy = centroid[1] - point[1];
        float dz = centroid[2] - point[2];

        float length = (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
        if (length > 0.001f) {
            return new float[]{dx/length, dy/length, dz/length};
        }

        return new float[]{0, 1, 0};
    }

    /**
     * 检查三角形有效性
     */
    private static boolean isValidTriangle(float[] p1, float[] p2, float[] p3) {
        // 检查边长是否合理
        float d12 = distance(p1, p2);
        float d23 = distance(p2, p3);
        float d31 = distance(p3, p1);

        float maxEdge = Math.max(d12, Math.max(d23, d31));
        float minEdge = Math.min(d12, Math.min(d23, d31));

        // 避免太长的边和太短的边
        boolean valid = maxEdge < 0.3f && minEdge > 0.01f && maxEdge / minEdge < 10.0f;

        // 检查三角形面积（避免退化三角形）
        if (valid) {
            float area = calculateTriangleArea(p1, p2, p3);
            valid = area > 0.0001f;
        }

        return valid;
    }

    /**
     * 计算三角形面积
     */
    private static float calculateTriangleArea(float[] p1, float[] p2, float[] p3) {
        // 计算两个边向量
        float[] v1 = {p2[0] - p1[0], p2[1] - p1[1], p2[2] - p1[2]};
        float[] v2 = {p3[0] - p1[0], p3[1] - p1[1], p3[2] - p1[2]};

        // 计算叉积
        float[] cross = {
                v1[1] * v2[2] - v1[2] * v2[1],
                v1[2] * v2[0] - v1[0] * v2[2],
                v1[0] * v2[1] - v1[1] * v2[0]
        };

        // 面积 = 叉积模长的一半
        float area = (float)Math.sqrt(cross[0]*cross[0] + cross[1]*cross[1] + cross[2]*cross[2]) / 2.0f;
        return area;
    }

    private static float distance(float[] p1, float[] p2) {
        float dx = p1[0] - p2[0];
        float dy = p1[1] - p2[1];
        float dz = p1[2] - p2[2];
        return (float)Math.sqrt(dx*dx + dy*dy + dz*dz);
    }

    /**
     * 改进的快速重建 - 使用K最近邻
     */
    public static MeshData improvedReconstruct(PointCloudData pointCloud, int targetPoints, int kNeighbors) {
        Log.i(TAG, "Starting improved reconstruction with KNN...");
        long startTime = System.currentTimeMillis();

        // 1. 均匀取样
        PointCloudData sampled = UniformSampler.uniformSample(pointCloud, targetPoints);

        // 2. 使用KNN三角化
        MeshData meshData = knnTriangulation(sampled, kNeighbors);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Improved reconstruction completed in %d ms: %d vertices, %d triangles",
                (endTime - startTime), meshData.vertices.size(), meshData.triangles.size()));

        return meshData;
    }

    /**
     * K最近邻三角化
     */
    private static MeshData knnTriangulation(PointCloudData pointCloud, int k) {
        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        vertices.addAll(pointCloud.points);

        // 为每个点计算法线
        for (int i = 0; i < vertices.size(); i++) {
            normals.add(calculateSimpleNormal(pointCloud, i));
        }

        // 使用KNN生成三角形
        generateKNNTriangles(pointCloud, vertices, triangles, k);

        return new MeshData(vertices, normals, triangles);
    }

    /**
     * 生成KNN三角形
     */
    private static void generateKNNTriangles(PointCloudData pointCloud,
                                             List<float[]> vertices,
                                             List<int[]> triangles,
                                             int k) {
        int maxTriangles = Math.min(150000, vertices.size() * 3);

        for (int i = 0; i < vertices.size() && triangles.size() < maxTriangles; i++) {
            float[] point = vertices.get(i);
            List<Integer> neighbors = findClosestNeighbors(pointCloud, point, k);

            // 为每个邻近点对创建三角形
            for (int j = 0; j < neighbors.size() && triangles.size() < maxTriangles; j++) {
                for (int m = j + 1; m < neighbors.size() && triangles.size() < maxTriangles; m++) {
                    int idx1 = neighbors.get(j);
                    int idx2 = neighbors.get(m);

                    // 避免重复三角形
                    if (i < idx1 && i < idx2) {
                        if (isValidTriangle(point, vertices.get(idx1), vertices.get(idx2))) {
                            triangles.add(new int[]{i, idx1, idx2});
                        }
                    }
                }
            }
        }

        Log.i(TAG, "KNN generated " + triangles.size() + " triangles with k=" + k);
    }

    /**
     * 超快速重建 - 用于实时预览
     */
    public static MeshData ultraFastReconstruct(PointCloudData pointCloud, int targetPoints) {
        Log.i(TAG, "Starting ultra fast reconstruction...");
        long startTime = System.currentTimeMillis();

        // 极速取样
        PointCloudData sampled = UniformSampler.uniformSample(pointCloud, Math.min(targetPoints, 5000));

        List<float[]> vertices = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        List<int[]> triangles = new ArrayList<>();

        vertices.addAll(sampled.points);

        // 所有点使用默认法线
        for (int i = 0; i < vertices.size(); i++) {
            normals.add(new float[]{0, 1, 0});
        }

        // 极简三角化：网格方式连接
        generateGridTriangles(vertices, triangles);

        long endTime = System.currentTimeMillis();
        Log.i(TAG, String.format("Ultra fast reconstruction completed in %d ms: %d vertices, %d triangles",
                (endTime - startTime), vertices.size(), triangles.size()));

        return new MeshData(vertices, normals, triangles);
    }

    /**
     * 网格方式生成三角形
     */
    private static void generateGridTriangles(List<float[]> vertices, List<int[]> triangles) {
        // 简单的网格连接
        int gridSize = (int) Math.sqrt(vertices.size());
        if (gridSize < 2) return;

        for (int i = 0; i < gridSize - 1; i++) {
            for (int j = 0; j < gridSize - 1; j++) {
                int idx1 = i * gridSize + j;
                int idx2 = i * gridSize + j + 1;
                int idx3 = (i + 1) * gridSize + j;
                int idx4 = (i + 1) * gridSize + j + 1;

                if (idx4 < vertices.size()) {
                    // 两个三角形组成一个网格面
                    triangles.add(new int[]{idx1, idx2, idx3});
                    triangles.add(new int[]{idx2, idx4, idx3});
                }
            }
        }
    }

    // 邻居辅助类
    private static class Neighbor {
        int index;
        float distance;

        Neighbor(int index, float distance) {
            this.index = index;
            this.distance = distance;
        }
    }
}
