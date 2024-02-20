#ifdef GL_ES
	#define PRECISION mediump
	precision PRECISION float
precision PRECISION int;
#else
	#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;
uniform float ht_segs;
uniform float ht_opacity;

// anti-alias smoothstep by mattdesl, https://github.com/glslify/glsl-aastep
float aastep(float threshold, float value) {
#ifdef GL_OES_standard_derivatives
    float afwidth = length(vec2(dFdx(value), dFdy(value))) * 0.70710678118654757;
return smoothstep(threshold-afwidth, threshold+afwidth, value);
#else
    return step(threshold, value);
#endif
}

// simplex noise (2d) by patriciogonzalezvivo, https://gist.github.com/patriciogonzalezvivo/670c22f3966e662d2f83
vec3 permute(vec3 x) { return mod(((x*34.0)+1.0)*x, 289.0); }

// halftone shader by mattdesl, https://github.com/glslify/glsl-halftone
float snoise(vec2 v){
const vec4 C = vec4(0.211324865405187, 0.366025403784439,
-0.577350269189626, 0.024390243902439);
vec2 i  = floor(v + dot(v, C.yy) );
vec2 x0 = v -   i + dot(i, C.xx);
vec2 i1;
i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);
vec4 x12 = x0.xyxy + C.xxzz;
x12.xy -= i1;
i = mod(i, 289.0);
vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))
+ i.x + vec3(0.0, i1.x, 1.0 ));
vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy),
dot(x12.zw,x12.zw)), 0.0);
m = m*m ;
m = m*m ;
vec3 x = 2.0 * fract(p * C.www) - 1.0;
vec3 h = abs(x) - 0.5;
vec3 ox = floor(x + 0.5);
vec3 a0 = x - ox;
m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );
vec3 g;
g.x  = a0.x  * x0.x  + h.x  * x0.y;
g.yz = a0.yz * x12.xz + h.yz * x12.yw;
return 130.0 * dot(m, g);
}

vec3 halftone(vec3 texcolor, vec2 st, float frequency) {
float n = 0.1*snoise(st*200.0); // Fractal noise
n += 0.05*snoise(st*400.0);
n += 0.025*snoise(st*800.0);
vec3 white = vec3(n*0.2 + 0.97);
vec3 black = vec3(n + 0.1);

// Perform a rough RGB-to-CMYK conversion
vec4 cmyk;
cmyk.xyz = 1.0 - texcolor;
cmyk.w = min(cmyk.x, min(cmyk.y, cmyk.z)); // Create K
cmyk.xyz -= cmyk.w; // Subtract K equivalent from CMY

// Distance to nearest point in a grid of
// (frequency x frequency) points over the unit square
vec2 Kst = frequency*mat2(0.707, -0.707, 0.707, 0.707)*st;
vec2 Kuv = 2.0*fract(Kst)-1.0;
float k = aastep(0.0, sqrt(cmyk.w)-length(Kuv)+n);
vec2 Cst = frequency*mat2(0.966, -0.259, 0.259, 0.966)*st;
vec2 Cuv = 2.0*fract(Cst)-1.0;
float c = aastep(0.0, sqrt(cmyk.x)-length(Cuv)+n);
vec2 Mst = frequency*mat2(0.966, 0.259, -0.259, 0.966)*st;
vec2 Muv = 2.0*fract(Mst)-1.0;
float m = aastep(0.0, sqrt(cmyk.y)-length(Muv)+n);
vec2 Yst = frequency*st; // 0 deg
vec2 Yuv = 2.0*fract(Yst)-1.0;
float y = aastep(0.0, sqrt(cmyk.z)-length(Yuv)+n);

vec3 rgbscreen = 1.0 - 0.9*vec3(c,m,y) + n;
return mix(rgbscreen, black, 0.85*k + 0.3*n);
}

vec3 halftone(vec3 texcolor, vec2 st) {
return halftone(texcolor, st, 30.0);
}

void main() {
vec2 uv = v_texCoords;
vec4 texcolor = texture2D(u_texture0, uv);
vec3 halftoneColor = halftone(texcolor.rgb, uv, ht_segs);

vec3 blendedColor = mix(texcolor.rgb, halftoneColor, ht_opacity);
vec3 darkBlendColor = clamp(blendedColor * 0.8, 0.0, 1.0);

gl_FragColor = vec4(darkBlendColor, 1.0);
}