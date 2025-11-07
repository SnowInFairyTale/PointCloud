#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 计算当前片段在点内的位置（从中心到边缘的距离）
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float distance = length(coord);

    // 如果是圆形点，丢弃边缘像素
    if (distance > 1.0) {
        discard;
    }

    fragColor = vColor;
}