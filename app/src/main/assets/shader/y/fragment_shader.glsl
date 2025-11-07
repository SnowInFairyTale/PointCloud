#version 300 es
precision mediump float;

in vec4 vColor;
out vec4 fragColor;

void main() {
    // 高效圆形点计算
    vec2 coord = gl_PointCoord - vec2(0.5);
    if (dot(coord, coord) > 0.25) {
        discard;
    }

    fragColor = vColor;
}