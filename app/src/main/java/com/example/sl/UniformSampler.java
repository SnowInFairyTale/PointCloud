package com.example.sl;

import android.util.Log;
import java.util.*;

public class UniformSampler {
    private static final String TAG = "UniformSampler";

    /**
     * 均匀取样 - 使用体素网格滤波实现真正均匀取样
     */
    public static PointCloudData uniformSample(PointCloudData pointCloud, int targetPoints) {
        if (pointCloud.pointCount <= targetPoints) {
            return pointCloud;
        }

        Log.i(TAG, "Uniform sampling from " + pointCloud.pointCount + " to " + targetPoints + " points");
        long startTime = System.currentTimeMillis();

        // 计算合适的体素大小
        float voxelSize = calculateOptimalVoxelSize(pointCloud, targetPoints);

        // 体素网格滤波
        PointCloudData sampled = voxelGridFilter(pointCloud, voxelSize);

        // 如果取样后点数还是太多，递归取样
        if (sampled.pointCount > targetPoints * 1.2f) {
            sampled = uniformSample(sampled, targetPoints);
        }
        // 如果取样后点数太少，使用随机补充
        else if (sampled.pointCount < targetPoints * 0.8f) {
            sampled = supplementWithRandom(pointCloud, sampled, targetPoints);
        }

        long endTime = System.currentTimeMillis();
        Log.i(TAG, "Uniform sampling completed: " + sampled.pointCount + " points in " + (endTime - startTime) + "ms");

        return sampled;
    }

    /**
     * 计算最优体素大小
     */
    private static float calculateOptimalVoxelSize(PointCloudData pointCloud, int targetPoints) {
        // 计算点云的包围盒体积
        float width = pointCloud.maxX - pointCloud.minX;
        float height = pointCloud.maxY - pointCloud.minY;
        float depth = pointCloud.maxZ - pointCloud.minZ;

        float volume = width * height * depth;

        // 计算每个体素应该包含的平均点数
        float pointsPerVoxel = (float) pointCloud.pointCount / targetPoints;

        // 计算体素大小：体积/目标体素数 = 每个体素的体积
        float voxelVolume = volume / (targetPoints / pointsPerVoxel);
        float voxelSize = (float) Math.pow(voxelVolume, 1.0/3.0);

        // 确保体素大小合理
        float minDimension = Math.min(width, Math.min(height, depth));
        voxelSize = Math.max(voxelSize, minDimension / 1000f); // 不能太小
        voxelSize = Math.min(voxelSize, minDimension / 10f);   // 不能太大

        Log.i(TAG, "Optimal voxel size: " + voxelSize + " (volume: " + volume + ")");
        return voxelSize;
    }

    /**
     * 体素网格滤波 - 每个体素保留一个点（质心）
     */
    private static PointCloudData voxelGridFilter(PointCloudData pointCloud, float voxelSize) {
        Map<String, Voxel> voxelMap = new HashMap<>();

        // 将点分配到体素中
        for (int i = 0; i < pointCloud.points.size(); i++) {
            float[] point = pointCloud.points.get(i);
            float[] color = pointCloud.colors.get(i);

            String voxelKey = getVoxelKey(point, voxelSize);

            if (!voxelMap.containsKey(voxelKey)) {
                voxelMap.put(voxelKey, new Voxel());
            }

            voxelMap.get(voxelKey).addPoint(point, color);
        }

        // 从每个体素中取质心点
        PointCloudData sampled = new PointCloudData();
        for (Voxel voxel : voxelMap.values()) {
            float[] centroid = voxel.getCentroid();
            float[] avgColor = voxel.getAverageColor();
            sampled.addPoint(centroid[0], centroid[1], centroid[2], avgColor[0], avgColor[1], avgColor[2]);
        }

        return sampled;
    }

    /**
     * 获取体素键值
     */
    private static String getVoxelKey(float[] point, float voxelSize) {
        int voxelX = (int) Math.floor(point[0] / voxelSize);
        int voxelY = (int) Math.floor(point[1] / voxelSize);
        int voxelZ = (int) Math.floor(point[2] / voxelSize);
        return voxelX + "," + voxelY + "," + voxelZ;
    }

    /**
     * 随机补充点数（当体素滤波后点数不足时）
     */
    private static PointCloudData supplementWithRandom(PointCloudData original,
                                                       PointCloudData sampled,
                                                       int targetPoints) {
        if (sampled.pointCount >= targetPoints) {
            return sampled;
        }

        int needed = targetPoints - sampled.pointCount;
        Random random = new Random();
        Set<Integer> usedIndices = new HashSet<>();

        // 从原始点云中随机选择未使用的点
        while (sampled.pointCount < targetPoints && usedIndices.size() < original.pointCount) {
            int randomIndex = random.nextInt(original.pointCount);

            if (!usedIndices.contains(randomIndex)) {
                usedIndices.add(randomIndex);

                float[] point = original.points.get(randomIndex);
                float[] color = original.colors.get(randomIndex);
                sampled.addPoint(point[0], point[1], point[2], color[0], color[1], color[2]);
            }
        }

        Log.i(TAG, "Supplemented with " + (sampled.pointCount - targetPoints + needed) + " random points");
        return sampled;
    }

    /**
     * 体素类 - 用于存储体素内的点信息
     */
    private static class Voxel {
        private List<float[]> points = new ArrayList<>();
        private List<float[]> colors = new ArrayList<>();
        private float sumX = 0, sumY = 0, sumZ = 0;
        private float sumR = 0, sumG = 0, sumB = 0;

        public void addPoint(float[] point, float[] color) {
            points.add(point);
            colors.add(color);

            sumX += point[0];
            sumY += point[1];
            sumZ += point[2];

            sumR += color[0];
            sumG += color[1];
            sumB += color[2];
        }

        public float[] getCentroid() {
            if (points.isEmpty()) {
                return new float[]{0, 0, 0};
            }
            return new float[]{
                    sumX / points.size(),
                    sumY / points.size(),
                    sumZ / points.size()
            };
        }

        public float[] getAverageColor() {
            if (colors.isEmpty()) {
                return new float[]{1, 1, 1};
            }
            return new float[]{
                    sumR / colors.size(),
                    sumG / colors.size(),
                    sumB / colors.size()
            };
        }
    }
}
