#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 计算当前片段在点内的位置
    vec2 coord = gl_PointCoord * 2.0 - 1.0;
    float distance = length(coord);

    // 丢弃圆形外的片段
    if (distance > 1.0) {
        discard;
    }

    // 简单的伪光照效果（中心亮，边缘暗）
    float light = 0.7 + 0.3 * (1.0 - distance);

    // 平滑边缘
    float edgeWidth = 0.3;
    float alpha = 1.0 - smoothstep(1.0 - edgeWidth, 1.0, distance);

    fragColor = vec4(vColor.rgb * light, vColor.a * alpha);
}