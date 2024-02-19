/*
    CRT-interlaced

    Copyright (C) 2010-2012 cgwg, Themaister and DOLLS

    This program is free software; you can redistribute it and/or modify it
    under the terms of the GNU General Public License as published by the Free
    Software Foundation; either version 2 of the License, or (at your option)
    any later version.

    (cgwg gave their consent to have the original version of this shader
    distributed under the GPL in this message:

        http://board.byuu.org/viewtopic.php?p=26075#p26075

        "Feel free to distribute my shaders under the GPL. After all, the
        barrel distortion code was taken from the Curvature shader, which is
        under the GPL."
    )
	This shader variant is pre-configured with screen curvature
*/
uniform mat4 u_projTrans;

#pragma parameter CRTgamma "CRTGeom Target Gamma" 2.4 0.1 5.0 0.1
#pragma parameter INV "Inverse Gamma/CRT-Geom Gamma out" 1.0 0.0 1.0 1.0
#pragma parameter monitorgamma "CRTGeom Monitor Gamma" 2.2 0.1 5.0 0.1
#pragma parameter d "CRTGeom Distance" 1.6 0.1 3.0 0.1
#pragma parameter CURVATURE "CRTGeom Curvature Toggle" 1.0 0.0 1.0 1.0
#pragma parameter R "CRTGeom Curvature Radius" 2.0 0.1 10.0 0.1
#pragma parameter cornersize "CRTGeom Corner Size" 0.03 0.001 1.0 0.005
#pragma parameter cornersmooth "CRTGeom Corner Smoothness" 1000.0 80.0 2000.0 100.0
#pragma parameter x_tilt "CRTGeom Horizontal Tilt" 0.0 -0.5 0.5 0.05
#pragma parameter y_tilt "CRTGeom Vertical Tilt" 0.0 -0.5 0.5 0.05
#pragma parameter overscan_x "CRTGeom Horiz. Overscan %" 100.0 -125.0 125.0 1.0
#pragma parameter overscan_y "CRTGeom Vert. Overscan %" 100.0 -125.0 125.0 1.0
#pragma parameter DOTMASK "CRTGeom Dot Mask Strength" 0.3 0.0 1.0 0.1
#pragma parameter SHARPER "CRTGeom Sharpness" 1.0 1.0 3.0 1.0
#pragma parameter scanline_weight "CRTGeom Scanline Weight" 0.3 0.1 0.5 0.05
#pragma parameter lum "CRTGeom Luminance" 0.0 0.0 1.0 0.01
#pragma parameter interlace_detect "CRTGeom Interlacing Simulation" 1.0 0.0 1.0 1.0
#pragma parameter SATURATION "CRTGeom Saturation" 1.0 0.0 2.0 0.05

#ifndef PARAMETER_UNIFORM
#define CRTgamma 2.4
#define monitorgamma 2.2
#define d 1.6
#define CURVATURE 1.0
#define R 2.0
#define cornersize 0.03
#define cornersmooth 1000.0
#define x_tilt 0.0
#define y_tilt 0.0
#define overscan_x 100.0
#define overscan_y 100.0
#define DOTMASK 0.3
#define SHARPER 1.0
#define scanline_weight 0.3
#define lum 0.0
#define interlace_detect 1.0
#define SATURATION 1.0
#define INV 1.0
#endif

#if __VERSION__ >= 130
#define COMPAT_VARYING out
#define COMPAT_ATTRIBUTE in
#define COMPAT_TEXTURE texture
#else
#define COMPAT_VARYING varying 
#define COMPAT_ATTRIBUTE attribute 
#define COMPAT_TEXTURE texture2D
#endif

#ifdef GL_ES
#define COMPAT_PRECISION mediump
#else
#define COMPAT_PRECISION
#endif

COMPAT_ATTRIBUTE vec4 VertexCoord;
COMPAT_ATTRIBUTE vec4 COLOR;
COMPAT_ATTRIBUTE vec4 TexCoord;
COMPAT_VARYING vec4 COL0;
COMPAT_VARYING vec4 TEX0;

vec4 _oPosition1;
uniform mat4 MVPMatrix;
uniform COMPAT_PRECISION int FrameDirection;
uniform COMPAT_PRECISION int FrameCount;
uniform COMPAT_PRECISION vec2 OutputSize;
uniform COMPAT_PRECISION vec2 TextureSize;
uniform COMPAT_PRECISION vec2 InputSize;

COMPAT_VARYING vec2 overscan;
COMPAT_VARYING vec2 aspect;
COMPAT_VARYING vec3 stretch;
COMPAT_VARYING vec2 sinangle;
COMPAT_VARYING vec2 cosangle;
COMPAT_VARYING vec2 one;
COMPAT_VARYING float mod_factor;
COMPAT_VARYING vec2 ilfac;

#ifdef PARAMETER_UNIFORM
uniform COMPAT_PRECISION float CRTgamma;
uniform COMPAT_PRECISION float monitorgamma;
uniform COMPAT_PRECISION float d;
uniform COMPAT_PRECISION float CURVATURE;
uniform COMPAT_PRECISION float R;
uniform COMPAT_PRECISION float cornersize;
uniform COMPAT_PRECISION float cornersmooth;
uniform COMPAT_PRECISION float x_tilt;
uniform COMPAT_PRECISION float y_tilt;
uniform COMPAT_PRECISION float overscan_x;
uniform COMPAT_PRECISION float overscan_y;
uniform COMPAT_PRECISION float DOTMASK;
uniform COMPAT_PRECISION float SHARPER;
uniform COMPAT_PRECISION float scanline_weight;
uniform COMPAT_PRECISION float lum;
uniform COMPAT_PRECISION float interlace_detect;
uniform COMPAT_PRECISION float SATURATION;
#endif

#define FIX(c) max(abs(c), 1e-5);

float intersect(vec2 xy)
{
float A = dot(xy,xy)+d*d;
float B = 2.0*(R*(dot(xy,sinangle)-d*cosangle.x*cosangle.y)-d*d);
float C = d*d + 2.0*R*d*cosangle.x*cosangle.y;
return (-B-sqrt(B*B-4.0*A*C))/(2.0*A);
}

vec2 bkwtrans(vec2 xy)
{
float c = intersect(xy);
vec2 point = vec2(c)*xy;
point -= vec2(-R)*sinangle;
point /= vec2(R);
vec2 tang = sinangle/cosangle;
vec2 poc = point/cosangle;
float A = dot(tang,tang)+1.0;
float B = -2.0*dot(poc,tang);
float C = dot(poc,poc)-1.0;
float a = (-B+sqrt(B*B-4.0*A*C))/(2.0*A);
vec2 uv = (point-a*sinangle)/cosangle;
float r = R*acos(a);
return uv*r/sin(r/R);
}

vec2 fwtrans(vec2 uv)
{
float r = FIX(sqrt(dot(uv,uv)));
uv *= sin(r/R)/r;
float x = 1.0-cos(r/R);
float D = d/R + x*cosangle.x*cosangle.y+dot(uv,sinangle);
return d*(uv*cosangle-x*sinangle)/D;
}

vec3 maxscale()
{
vec2 c = bkwtrans(-R * sinangle / (1.0 + R/d*cosangle.x*cosangle.y));
vec2 a = vec2(0.5,0.5)*aspect;
vec2 lo = vec2(fwtrans(vec2(-a.x,c.y)).x, fwtrans(vec2(c.x,-a.y)).y)/aspect;
vec2 hi = vec2(fwtrans(vec2(+a.x,c.y)).x, fwtrans(vec2(c.x,+a.y)).y)/aspect;
return vec3((hi+lo)*aspect*0.5,max(hi.x-lo.x,hi.y-lo.y));
}

void main()
{
// START of parameters

// gamma of simulated CRT
//  CRTgamma = 1.8;
// gamma of display monitor (typically 2.2 is correct)
//  monitorgamma = 2.2;
// overscan (e.g. 1.02 for 2% overscan)
overscan = vec2(1.00,1.00);
// aspect ratio
aspect = vec2(1.0, 0.75);
// lengths are measured in units of (approximately) the width
// of the monitor simulated distance from viewer to monitor
//  d = 2.0;
// radius of curvature
//  R = 1.5;
// tilt angle in radians
// (behavior might be a bit wrong if both components are
// nonzero)
const vec2 angle = vec2(0.0,0.0);
// size of curved corners
//  cornersize = 0.03;
// border smoothness parameter
// decrease if borders are too aliased
//  cornersmooth = 1000.0;

// END of parameters

vec4 _oColor;
vec2 _otexCoord;
gl_Position = VertexCoord.x * MVPMatrix[0] + VertexCoord.y * MVPMatrix[1] + VertexCoord.z * MVPMatrix[2] + VertexCoord.w * MVPMatrix[3];
_oPosition1 = gl_Position;
_oColor = COLOR;
_otexCoord = TexCoord.xy*1.0001;
COL0 = COLOR;
TEX0.xy = TexCoord.xy*1.0001;

// Precalculate a bunch of useful values we'll need in the fragment
// shader.
sinangle = sin(vec2(x_tilt, y_tilt)) + vec2(0.001);//sin(vec2(max(abs(x_tilt), 1e-3), max(abs(y_tilt), 1e-3)));
cosangle = cos(vec2(x_tilt, y_tilt)) + vec2(0.001);//cos(vec2(max(abs(x_tilt), 1e-3), max(abs(y_tilt), 1e-3)));
stretch = maxscale();

ilfac = vec2(1.0,clamp(floor(InputSize.y/200.0), 1.0, 2.0));

// The size of one texel, in texture-coordinates.
vec2 sharpTextureSize = vec2(SHARPER * TextureSize.x, TextureSize.y);
one = ilfac / sharpTextureSize;

// Resulting X pixel-coordinate of the pixel we're drawing.
mod_factor = TexCoord.x * TextureSize.x * OutputSize.x / InputSize.x;

}

