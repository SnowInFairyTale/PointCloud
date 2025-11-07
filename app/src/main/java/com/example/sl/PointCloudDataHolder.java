package com.example.sl;

public class PointCloudDataHolder {
    private static PointCloudData pointCloudData;

    public static void setData(PointCloudData data) {
        pointCloudData = data;
    }

    public static PointCloudData getPointCloudData() {
        return pointCloudData;
    }
}
