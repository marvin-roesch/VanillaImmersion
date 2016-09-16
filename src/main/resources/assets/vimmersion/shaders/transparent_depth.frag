#version 120

uniform bool overrideDepth;
uniform sampler2D tex;

void main() {
    vec4 color = texture2D(tex, vec2(gl_TexCoord[0])) * gl_Color;
    if (color.a == 0 && overrideDepth) {
        gl_FragDepth = 1.0 / 0.0;
    } else {
        gl_FragDepth = gl_FragCoord.z;
    }
    gl_FragColor = color;
}
