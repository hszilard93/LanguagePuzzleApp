#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_texCoord;
uniform sampler2D u_texture;
uniform vec4 u_glowColor;
uniform float u_glowIntensity;
uniform float u_resolution; // Texture resolution for scaling offsets

void main() {
    vec4 color = texture2D(u_texture, v_texCoord);
    float alpha = color.a;

    float glowRadius = 1.5;

    // Normalized offsets based on resolution
    float offset = glowRadius / u_resolution;

    float dx = 0.0;
    float dy = 0.0;

    // Sample neighboring pixels for gradient (central differences)
    dx = texture2D(u_texture, v_texCoord + vec2(offset, 0.0)).a - texture2D(u_texture, v_texCoord - vec2(offset, 0.0)).a;
    dy = texture2D(u_texture, v_texCoord + vec2(0.0, offset)).a - texture2D(u_texture, v_texCoord - vec2(0.0, offset)).a;

    // Compute the gradient magnitude
    float gradient = sqrt(dx * dx + dy * dy);

    // Normalize and apply a smoothstep for soft transition
    float edge = smoothstep(0.1, 0.3, gradient);

    // Calculate glow intensity with falloff and restrict to transparent areas
    float glow = edge * u_glowIntensity * (1.0 - alpha);

    // Apply the glow color
    vec4 finalGlow = u_glowColor * glow;

    // Combine the original color with the glow
    gl_FragColor = color + finalGlow;
}
