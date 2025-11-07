#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;

uniform mat4 uMVPMatrix;
uniform float uPointSize;  // 动态点大小控制

out vec4 vColor;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);

    // 根据距离动态调整点大小，保持透视效果
    float depth = clamp(length(aPosition) * 0.3, 0.5, 2.0);
    gl_PointSize = uPointSize * depth;

    vColor = aColor;
}