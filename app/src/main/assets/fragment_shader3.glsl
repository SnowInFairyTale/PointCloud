#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 简单的点渲染
    fragColor = vColor;

    // 可选：为点添加圆形效果（避免方形点）
    vec2 coord = gl_PointCoord - vec2(0.5);
    if (length(coord) > 0.5) {
        discard;
    }
}