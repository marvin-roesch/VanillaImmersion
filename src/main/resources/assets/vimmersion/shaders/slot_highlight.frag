#version 120

uniform sampler2D tex;

void main() {
    vec4 texColor = texture2D(tex, vec2(gl_TexCoord[0]));
    float average = (texColor.r + texColor.g + texColor.b) / 3.0;
    vec4 color = vec4(vec3(average), texColor.a);
    gl_FragColor = color * gl_Color;
}