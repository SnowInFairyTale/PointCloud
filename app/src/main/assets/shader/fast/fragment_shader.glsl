#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 直接输出颜色，方形点（性能最好）
    fragColor = vColor;
}