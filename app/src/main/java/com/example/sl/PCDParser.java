package com.example.sl;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class PCDParser {
    private static final String TAG = "PCDParser";

    public static PointCloudData parsePCDFromAssets(Context context, String filename) {
        PointCloudData data = new PointCloudData();

        try {
            InputStream inputStream = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            boolean inHeader = true;
            boolean dataStarted = false;
            int pointsCount = 0;
            boolean hasRGB = false;

            while ((line = reader.readLine()) != null) {
                if (inHeader) {
                    if (line.startsWith("FIELDS")) {
                        hasRGB = line.contains("rgb");
                        Log.i(TAG, "File has RGB field: " + hasRGB);
                    } else if (line.startsWith("POINTS")) {
                        String[] parts = line.split("\\s+");
                        pointsCount = Integer.parseInt(parts[1]);
                        Log.i(TAG, "Total points: " + pointsCount);
                    } else if (line.startsWith("DATA ascii")) {
                        inHeader = false;
                        dataStarted = true;
                        continue;
                    }
                }

                if (dataStarted) {
                    String[] values = line.trim().split("\\s+");
                    if (values.length >= 4) {
                        float x = Float.parseFloat(values[0]);
                        float y = Float.parseFloat(values[1]);
                        float z = Float.parseFloat(values[2]);

                        if (hasRGB && values.length >= 4) {
                            // 解析打包的RGB值
                            long packedRGB = Long.parseLong(values[3]);
                            float[] rgb = unpackRGB(packedRGB);
                            data.addPoint(x, y, z, rgb[0], rgb[1], rgb[2]);
                        } else {
                            // 没有颜色信息，使用高度颜色
                            data.addPoint(x, y, z);
                        }
                    }

                    // 显示进度（对于大文件）
                    if (data.pointCount % 100000 == 0) {
                        Log.i(TAG, "Parsed " + data.pointCount + " points...");
                    }

                    if (data.pointCount >= pointsCount) {
                        break;
                    }
                }
            }

            reader.close();
            Log.i(TAG, "Successfully parsed " + data.pointCount + " points");

        } catch (IOException e) {
            Log.e(TAG, "Error reading PCD file: " + e.getMessage());
            e.printStackTrace();
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing number: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    /**
     * 将32位打包的RGB值解包为浮点数数组 [r, g, b]
     * 格式通常是：0xFF0000 (红), 0x00FF00 (绿), 0x0000FF (蓝)
     */
    private static float[] unpackRGB(long packedRGB) {
        float r = ((packedRGB >> 16) & 0xFF) / 255.0f;
        float g = ((packedRGB >> 8) & 0xFF) / 255.0f;
        float b = (packedRGB & 0xFF) / 255.0f;

        return new float[]{r, g, b};
    }
}