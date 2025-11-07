#version 300 es
precision highp float;  // 使用高精度

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 高质量圆形点计算
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float distance = length(coord);

    // 平滑边缘过渡
    float smoothEdge = 0.1;
    float alpha = 1.0 - smoothstep(1.0 - smoothEdge, 1.0, distance);

    // 轻微的内发光效果
    float innerGlow = 0.3 + 0.7 * (1.0 - distance);

    // 最终颜色
    fragColor = vec4(vColor.rgb * innerGlow, vColor.a * alpha);

    // 完全丢弃圆形外的像素
    if (distance > 1.0) {
        discard;
    }
}