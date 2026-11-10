#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform float specularStrength;

uniform sampler2D textureSampler;   // texture unit 0
uniform sampler2D shadowMap;        // texture unit 1
uniform mat4 lightSpaceMatrix;

uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadows;
uniform bool unlit;                 // dipakai untuk penanda posisi cahaya

// Shadow Mapping: bandingkan kedalaman fragment (sudut pandang cahaya) dengan Shadow Map.
// Mengembalikan 0.0 (terang) .. 1.0 (bayangan penuh). PCF 3x3 agar tepi bayangan halus.
float shadowAmount(vec3 norm, vec3 lightDir) {
    float NdotL = max(dot(norm, lightDir), 0.0);

    // Normal-offset bias: geser posisi sedikit sepanjang normal (mengurangi Shadow Acne)
    vec3 offsetPos = FragPos + norm * (0.06 * (1.0 - NdotL));
    vec4 fragPosLight = lightSpaceMatrix * vec4(offsetPos, 1.0);

    vec3 proj = fragPosLight.xyz / fragPosLight.w;   // perspective divide (aman untuk ortho)
    proj = proj * 0.5 + 0.5;                         // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;

    // Slope-scaled bias: makin miring permukaan terhadap cahaya, makin besar bias
    float bias = max(0.004 * (1.0 - NdotL), 0.0008);
    float current = proj.z;

    float shadow = 0.0;
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float closest = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (current - bias > closest) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb : objectColor;
    if (unlit) {
        FragColor = vec4(objectColor, 1.0);
        return;
    }

    vec3 norm = normalize(Normal);
    if (!gl_FrontFacing) norm = -norm;   // sisi belakang (saat culling dimatikan) tetap ternormal

    // 1. Ambient Lighting
    float ambientStrength = 0.25;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse Lighting (Lambertian)
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular Lighting (Phong vs Blinn-Phong)
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (diff > 0.0) {
        if (useBlinnPhong) {
            vec3 halfwayDir = normalize(lightDir + viewDir);
            spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);   // Blinn-Phong
        } else {
            vec3 reflectDir = reflect(-lightDir, norm);
            spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0); // Standard Phong
        }
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow (hanya mempengaruhi diffuse + specular, ambient tetap)
    float shadow = useShadows ? shadowAmount(norm, lightDir) : 0.0;

    vec3 result = (ambient + (1.0 - shadow) * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
