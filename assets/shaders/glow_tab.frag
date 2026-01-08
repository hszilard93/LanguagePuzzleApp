#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoord;
uniform sampler2D u_texture;
uniform vec4 u_glowColor;
uniform float u_glowIntensity;
uniform float u_resolution; // Texture resolution for scaling offsets

void main() {
    // Sample the texture color
    vec4 textureColor = texture2D(u_texture, v_texCoord);

    // Multiply by the vertex color (batch.color)
    vec4 color = v_color * textureColor;
    float alpha = color.a;

    // Glow effect computations (same as before)
    float glowRadius = 1.0;
    float offset = glowRadius / u_resolution;

    float dx = texture2D(u_texture, v_texCoord + vec2(offset, 0.0)).a - texture2D(u_texture, v_texCoord - vec2(offset, 0.0)).a;
    float dy = texture2D(u_texture, v_texCoord + vec2(0.0, offset)).a - texture2D(u_texture, v_texCoord - vec2(0.0, offset)).a;
    float gradient = sqrt(dx * dx + dy * dy);
    float edge = smoothstep(0.1, 0.3, gradient);
    float glow = edge * u_glowIntensity * (1.0 - alpha);
    vec4 finalGlow = u_glowColor * glow;

    // Combine the original color with the glow
    gl_FragColor = color * 1.5 + finalGlow;
}
