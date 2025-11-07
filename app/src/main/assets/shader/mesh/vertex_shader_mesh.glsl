#version 300 es
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aTexCoord;

uniform mat4 uMVPMatrix;
uniform mat4 uModelMatrix;
uniform mat4 uNormalMatrix;

out vec3 vNormal;
out vec3 vFragPos;
out vec2 vTexCoord;

void main() {
    gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
    vFragPos = vec3(uModelMatrix * vec4(aPosition, 1.0));
    vNormal = mat3(uNormalMatrix) * aNormal;
    vTexCoord = aTexCoord;
}