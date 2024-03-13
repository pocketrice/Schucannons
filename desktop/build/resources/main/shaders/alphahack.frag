#ifdef GL_ES
precision mediump float;
#endif

uniform sampler2D u_texture;
uniform vec4 oldColor;
uniform vec4 newColor;

varying vec2 v_texCoords;

void main() {
	// Sample the texture color at the current texture coordinate
	vec4 textureColor = texture2D(u_texture, v_texCoords);

	// Check if the texture color matches the background color
	if (textureColor == oldColor) {
		// If the texture color matches the background color, output new color
		gl_FragColor = newColor;
	} else {
		// If the texture color doesn't match the background color, output original texture color
		gl_FragColor = textureColor;
	}
}
