#version 300 es
precision mediump float;

in vec3 vNormal;
in vec3 vFragPos;
in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform vec3 uLightPosition;

out vec4 fragColor;

void main() {
    // 环境光
    vec3 ambient = vec3(0.3);

    // 漫反射
    vec3 norm = normalize(vNormal);
    vec3 lightDir = normalize(uLightPosition - vFragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = vec3(0.8) * diff;

    // 镜面反射
    vec3 viewDir = normalize(-vFragPos);
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), 32.0);
    vec3 specular = vec3(0.5) * spec;

    // 纹理颜色
    vec4 textureColor = texture(uTexture, vTexCoord);

    // 最终颜色
    vec3 result = (ambient + diffuse + specular) * textureColor.rgb;
    fragColor = vec4(result, 1.0);
}