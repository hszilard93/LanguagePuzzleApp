#ifdef GL_ES
precision mediump float;
#endif

varying vec4 v_color;
varying vec2 v_texCoords;
uniform sampler2D u_texture;
uniform vec2 u_textureSize;

bool isSameColor(vec4 color1, vec4 color2) {
return all(equal(color1.rgba, color2.rgba));
}

bool isGrey(vec4 color) {
// vec4 black = vec4(255, 255, 255, 1.0);
// return all(equal(color.rgb, black.rgb));
if (color.r == color.g == color.b) {
return true;
}
else {
return false;
}
}

void main() {
    vec4 texColor = texture2D(u_texture, v_texCoords);
    if (texColor.a <= 0.9) {
        vec2 pixelSize = 1.0 / u_textureSize;
        int sameColorCount = 0;

        const int neighborCount = 24;
        vec4 neighborColors[neighborCount];
        int colorI = 0;
        for (int y = -2; y <= 2; y++) {
            for (int x = -2; x <= 2; x++) {
                if (x == 0 || y == 0) continue;

                /* bool isXClose = x == -1 || x == 0 || x == 0;
                bool isYClose = y == -1 || y == 0 || y == -1;
                if (isXClose && isYClose) continue; // Skip the center pixel block */

                vec2 neighborCoord = v_texCoords + vec2(float(x), float(y)) * pixelSize;
                if (neighborCoord.x < 0.0 || neighborCoord.x > 1.0 || neighborCoord.y < 0.0 || neighborCoord.y > 1.0) {
                    continue; // Skip this neighbor if out of bounds
                }

                vec4 neighborColor = texture2D(u_texture, neighborCoord);
                neighborColors[colorI] = neighborColor;
                colorI += 1;
            }
        }

        int numberOfSolidPixels = 0;
        for (int i = 0; i < neighborCount; i++) {
            if (neighborColors[i].a == 1.0) {
                numberOfSolidPixels += 1;
            }
        }
        if (numberOfSolidPixels >= 8) {
            // hasEnoughSolidPixels = true;
            // texColor.rgb = neighborColors[i].rgb;
            // texColor.a = 0.5;

            // texColor.r = 255;
            // texColor.g = 0;
            // texColor.b = 255;
            texColor.a = 1.0;
        }

        /*for (int y = -1; y <= 1; y++) {
            for (int x = -1; x <= 1; x++) {
                if (x == 0 && y == 0) continue;

                vec2 neighborCoord = v_texCoords + vec2(float(x), float(y)) * pixelSize;
                vec4 neighborColor = texture2D(u_texture, neighborCoord);

                if (isSameColor(texColor, neighborColor)) {
                    sameColorCount++;
                }

                if (sameColorCount >= 6) {
                    texColor.a = 1.0;
                    break;
                }
            }
            if (sameColorCount >= 6) break;
        }*/
    }

    gl_FragColor = texColor * v_color;
}
