#version 120

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec4 Grade;
uniform vec3 Tint;
uniform vec3 Fx;
uniform vec3 Lx;
uniform vec4 Sun;

varying vec2 texCoord;

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

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

    float l0 = luma(c);

    // pseudo ambient occlusion: darken creases (pixels darker than their surroundings)
    if (Lx.y > 0.0) {
        float a = 0.0;
        vec2 r1 = px * 2.5;
        vec2 r2 = px * 6.0;
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(r1.x, 0.0)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord - vec2(r1.x, 0.0)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(0.0, r1.y)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord - vec2(0.0, r1.y)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(r2.x, r2.y)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(-r2.x, r2.y)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(r2.x, -r2.y)).rgb);
        a += luma(texture2D(DiffuseSampler, texCoord + vec2(-r2.x, -r2.y)).rgb);
        a *= 0.125;
        float ao = clamp((a - l0) * 2.2, 0.0, 0.6);
        c *= 1.0 - ao * Lx.y;
    }

    // sun light shafts (God rays) toward the sun position, set from the game each frame
    if (Lx.x > 0.0 && Sun.z > 0.0) {
        vec2 dir = (Sun.xy - texCoord) * 0.07;
        vec2 uv = texCoord;
        float w = 1.0;
        float acc = 0.0;
        for (int i = 0; i < 12; i++) {
            uv += dir;
            vec3 s = texture2D(DiffuseSampler, uv).rgb;
            acc += max(luma(s) - 0.72, 0.0) * w;
            w *= 0.9;
        }
        vec3 sunCol = mix(vec3(1.0, 0.93, 0.72), vec3(1.0, 0.55, 0.22), Sun.w);
        float glow = pow(max(1.0 - distance(texCoord, Sun.xy) * 1.4, 0.0), 3.0);
        c += sunCol * (acc * 0.5 + glow * 0.18) * Sun.z * Lx.x;
    }

    c *= Grade.z * Tint;
    // time of day warmth/coolness
    if (Lx.x > 0.0) c *= mix(vec3(1.0), vec3(1.08, 0.97, 0.86), Sun.w * 0.6 * Sun.z);
    if (Fx.y > 0.0) c = mix(c, aces(c), Fx.y);
    c = (c - 0.5) * Grade.y + 0.5;
    float l = dot(c, vec3(0.299, 0.587, 0.114));
    c = mix(vec3(l), c, Grade.x);
    float d = distance(texCoord, vec2(0.5));
    c *= 1.0 - Grade.w * smoothstep(0.35, 0.95, d);

    gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
