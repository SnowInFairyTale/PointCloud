package com.example.sl;

import java.util.ArrayList;
import java.util.List;

public class MeshData {
    public List<float[]> vertices;
    public List<float[]> normals;
    public List<float[]> texCoords;
    public List<int[]> triangles;
    public int textureId = 0;

    public MeshData(List<float[]> vertices, List<float[]> normals, List<int[]> triangles) {
        this.vertices = vertices;
        this.normals = normals;
        this.triangles = triangles;
        generateTextureCoordinates();
    }

    private void generateTextureCoordinates() {
        texCoords = new ArrayList<>();

        // 简单的平面投影纹理坐标
        for (float[] vertex : vertices) {
            float u = (vertex[0] + 1.0f) * 0.5f; // 映射到 [0,1]
            float v = (vertex[1] + 1.0f) * 0.5f;
            texCoords.add(new float[]{u, v});
        }
    }

    public float[] getVerticesArray() {
        float[] array = new float[vertices.size() * 3];
        for (int i = 0; i < vertices.size(); i++) {
            float[] vertex = vertices.get(i);
            array[i * 3] = vertex[0];
            array[i * 3 + 1] = vertex[1];
            array[i * 3 + 2] = vertex[2];
        }
        return array;
    }

    public float[] getNormalsArray() {
        float[] array = new float[normals.size() * 3];
        for (int i = 0; i < normals.size(); i++) {
            float[] normal = normals.get(i);
            array[i * 3] = normal[0];
            array[i * 3 + 1] = normal[1];
            array[i * 3 + 2] = normal[2];
        }
        return array;
    }

    public float[] getTexCoordsArray() {
        float[] array = new float[texCoords.size() * 2];
        for (int i = 0; i < texCoords.size(); i++) {
            float[] texCoord = texCoords.get(i);
            array[i * 2] = texCoord[0];
            array[i * 2 + 1] = texCoord[1];
        }
        return array;
    }

    public int[] getIndicesArray() {
        int totalIndices = triangles.size() * 3;
        int[] array = new int[totalIndices];
        int index = 0;

        for (int[] triangle : triangles) {
            array[index++] = triangle[0];
            array[index++] = triangle[1];
            array[index++] = triangle[2];
        }

        return array;
    }
}
