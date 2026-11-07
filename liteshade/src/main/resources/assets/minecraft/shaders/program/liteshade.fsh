#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec4 Grade;
uniform vec3 Tint;
uniform vec3 Fx;

varying vec2 texCoord;

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

void main() {
    vec2 px = 1.0 / InSize;
    vec3 c = texture2D(DiffuseSampler, texCoord).rgb;

    if (Fx.x > 0.0) {
        vec3 n = texture2D(DiffuseSampler, texCoord + vec2(px.x, 0.0)).rgb
               + texture2D(DiffuseSampler, texCoord - vec2(px.x, 0.0)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(0.0, px.y)).rgb
               + texture2D(DiffuseSampler, texCoord - vec2(0.0, px.y)).rgb;
        c += (c * 4.0 - n) * Fx.x * 0.5;
    }

    if (Fx.z > 0.0) {
        vec2 r = px * 5.0;
        vec3 b = texture2D(DiffuseSampler, texCoord + vec2(r.x, r.y)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(-r.x, r.y)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(r.x, -r.y)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(-r.x, -r.y)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(r.x * 2.0, 0.0)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(-r.x * 2.0, 0.0)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(0.0, r.y * 2.0)).rgb
               + texture2D(DiffuseSampler, texCoord + vec2(0.0, -r.y * 2.0)).rgb;
        b *= 0.125;
        c += max(b - vec3(0.62), vec3(0.0)) * Fx.z * 1.6;
    }

    c *= Grade.z * Tint;
    if (Fx.y > 0.0) c = mix(c, aces(c), Fx.y);
    c = (c - 0.5) * Grade.y + 0.5;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, Grade.x);
    float d = distance(texCoord, vec2(0.5));
    c *= 1.0 - Grade.w * smoothstep(0.35, 0.95, d);

    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
