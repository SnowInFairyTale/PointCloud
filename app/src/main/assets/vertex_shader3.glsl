#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec4 aColor;

uniform mat4 uMVPMatrix;

out vec4 vColor;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);

    // 根据距离调整点大小 - 近大远小
    float distance = length(aPosition);
    gl_PointSize = mix(1.5, 0.5, clamp(distance * 0.5, 0.0, 1.0));

    vColor = aColor;
}