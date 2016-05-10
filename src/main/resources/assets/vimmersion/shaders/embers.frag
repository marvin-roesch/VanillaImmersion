#version 120

uniform vec4 tint;
uniform float progress;
uniform sampler2D tex;

vec4 fade(vec4 a, vec4 b, float t) {
    return vec4(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t, a.z + (b.z - a.z) * t, a.w + (b.w - a.w) * t);
}

void main() {
    vec4 start = vec4(0, 0, 0, 0);
    vec4 end = vec4(0.8, 0.8, 0.8, 0);
    gl_FragColor = texture2D(tex, vec2(gl_TexCoord[0])) * gl_Color + fade(start, end, progress);
}