// Bitmap to ASCII (not really) fragment shader by movAX13h, September 2013
// This is the original shader that is now used in PixiJs, FL Studio and various other products.

// Here's a little tool for new characters: thrill-project.com/archiv/coding/bitmap/

// update 2018-12-14: values for characters are integer now (were float)
//                    since bit operations are available now, making use of them
//                    instead of int(mod(n/exp2(p.x + 5.0*p.y), 2.0))
// update 2023-04-21: added characters A-Z and 0-9 and some others
//                    black/white mode does not use gray value anymore

#ifdef GL_ES
	#define PRECISION mediump
	precision PRECISION float
precision PRECISION int;
#else
	#define PRECISION
#endif

varying vec2 v_texCoords;
uniform sampler2D u_texture0;

float character(float n, vec2 p)
{
p = floor(p*vec2(4.0, -4.0) + 2.5);
if (clamp(p.x, 0.0, 4.0) == p.x)
{
if (clamp(p.y, 0.0, 4.0) == p.y)
{
if (int(mod(n/exp2(p.x + 5.0*p.y), 2.0)) == 1) return 1.0;
}
}
return 0.0;
}

void main()
{
vec2 p = gl_FragCoord.xy;
vec2 uv = v_texCoords;

vec3 col = texture2D(u_texture0, uv).rgb;

float gray = 0.3 * col.r + 0.59 * col.g + 0.11 * col.b;

float n =  4096.0;              // .
//if (gray > 0.2) n = 65600.0;    // :
//if (gray > 0.3) n = 332772.0;   // *
//if (gray > 0.4) n = 15255086.0; // o
//if (gray > 0.5) n = 23385164.0; // &
//if (gray > 0.6) n = 15252014.0; // 8
//if (gray > 0.7) n = 13199452.0; // @
//if (gray > 0.8) n = 11512810.0; // #

if (gray > 0.0233) n = 4096.0;
if (gray > 0.0465) n = 131200.0;
if (gray > 0.0698) n = 4329476.0;
if (gray > 0.0930) n = 459200.0;
if (gray > 0.1163) n = 4591748.0;
if (gray > 0.1395) n = 12652620.0;
if (gray > 0.1628) n = 14749828.0;
if (gray > 0.1860) n = 18393220.0;
if (gray > 0.2093) n = 15239300.0;
if (gray > 0.2326) n = 17318431.0;
if (gray > 0.2558) n = 32641156.0;
if (gray > 0.2791) n = 18393412.0;
if (gray > 0.3023) n = 18157905.0;
if (gray > 0.3256) n = 17463428.0;
if (gray > 0.3488) n = 14954572.0;
if (gray > 0.3721) n = 13177118.0;
if (gray > 0.3953) n = 6566222.0;
if (gray > 0.4186) n = 16269839.0;
if (gray > 0.4419) n = 18444881.0;
if (gray > 0.4651) n = 18400814.0;
if (gray > 0.4884) n = 33061392.0;
if (gray > 0.5116) n = 15255086.0;
if (gray > 0.5349) n = 32045584.0;
if (gray > 0.5581) n = 18405034.0;
if (gray > 0.5814) n = 15022158.0;
if (gray > 0.6047) n = 15018318.0;
if (gray > 0.6279) n = 16272942.0;
if (gray > 0.6512) n = 18415153.0;
if (gray > 0.6744) n = 32641183.0;
if (gray > 0.6977) n = 32540207.0;
if (gray > 0.7209) n = 18732593.0;
if (gray > 0.7442) n = 18667121.0;
if (gray > 0.7674) n = 16267326.0;
if (gray > 0.7907) n = 32575775.0;
if (gray > 0.8140) n = 15022414.0;
if (gray > 0.8372) n = 15255537.0;
if (gray > 0.8605) n = 32032318.0;
if (gray > 0.8837) n = 32045617.0;
if (gray > 0.9070) n = 33081316.0;
if (gray > 0.9302) n = 32045630.0;
if (gray > 0.9535) n = 33061407.0;
if (gray > 0.9767) n = 11512810.0;

p = mod(p/4.0, 2.0) - vec2(1.0);
col = col*character(n, p);

gl_FragColor = vec4(col, 1.0);
}