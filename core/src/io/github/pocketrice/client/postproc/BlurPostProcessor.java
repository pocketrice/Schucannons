package io.github.pocketrice.client.postproc;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class BlurPostProcessor {
    private FrameBuffer result, blurTargetA, blurTargetB;
    private ShaderProgram blurShader, defaultShader;
    private int pingPongCount; // Higher val = greater GPU cost
    private float time, maxBlur, blurScale;
    private SpriteBatch postBatch; // would be most efficient to use same postBatch, but GDX-VFX expects same FBO.

    public BlurPostProcessor(SpriteBatch pb) {
        this(6,4f,0.7f, pb);
    }

    public BlurPostProcessor(int pingPongCount, float maxBlur, float blurScl, SpriteBatch pb) {
        this.pingPongCount = pingPongCount;
        this.maxBlur = maxBlur;
        blurScale = blurScl;
        time = 0f;

        postBatch = pb;
        defaultShader = postBatch.getShader();
        blurShader = buildShader("shaders/blur.vert", "shaders/blur.frag");
    }


    // adapted from JamesTKhan shader tutorial
    private ShaderProgram buildShader(String vertPath, String fragPath) {
        String vert = Gdx.files.internal(vertPath).readString();
        String frag = Gdx.files.internal(fragPath).readString();
        return compileShader(vert, frag);
    }

    private ShaderProgram compileShader(String vertCode, String fragCode) {
        ShaderProgram program = new ShaderProgram(vertCode, fragCode);

        if (!program.isCompiled()) {
            throw new GdxRuntimeException(program.getLog());
        }

        return program;
    }

    public Texture render(FrameBuffer fbo) {
        time += Gdx.graphics.getDeltaTime();

        // Get color texture from frame buffer
        Texture fboTex = fbo.getColorBufferTexture();

        // Apply blur and retrieve texture
        return postProcessRender(fboTex);
    }

    private Texture postProcessRender(Texture fboTex) {
        postBatch.setShader(blurShader);

        for (int i = 0; i < pingPongCount; i++) {
            // Horizontal blur pass
            blurTargetA.begin();
            postBatch.begin();
            blurShader.setUniformf("dir", 0.5f, 0); // horizontal dir
            blurShader.setUniformf("radius",  maxBlur);
            blurShader.setUniformf("resolution", Gdx.graphics.getWidth());
            postBatch.draw((i == 0) ? fboTex : blurTargetB.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();
            blurTargetA.end();

            // Vertical blur pass
            blurTargetB.begin();
            postBatch.begin();
            blurShader.setUniformf("dir", 0, 0.5f);
            blurShader.setUniformf("radius", maxBlur);
            blurShader.setUniformf("resolution", Gdx.graphics.getHeight());
            postBatch.draw(blurTargetA.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();
            blurTargetB.end();
        }

//        ShaderProgram.pedantic = false;
//        postBatch.setShader(fastDistortShader);
//        fastDistortShader.bind();
//        fastDistortShader.setUniformf("u_time", time);
//        distortTarget.begin();
//        postBatch.begin();
//        postBatch.draw(blurTrgtB.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0,1, 1);
//        postBatch.end();
//        distortTarget.end();

        postBatch.setShader(defaultShader);

        return blurTargetB.getColorBufferTexture();
    }

    public void buildFBO(int width, int height) {
        if (width == 0 || height == 0) return;

        if (result != null) result.dispose();
        if (blurTargetA != null) blurTargetA.dispose();
        if (blurTargetB != null) blurTargetB.dispose();
        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);

        // Enhanced precision, only needed for 3D scenes
        frameBufferBuilder.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT24);
        result = frameBufferBuilder.build();

        blurTargetA = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (width * blurScale), (int) (height * blurScale), false);
        blurTargetB = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (width * blurScale), (int) (height * blurScale), false);
    }

//    private ShaderProgram buildShader(String glslPath) {
//        String[] shaders = extractShaders(Gdx.files.internal(glslPath).readString());
//        ShaderProgram program = new ShaderProgram(shaders[0], shaders[1]);
//
//        if (!program.isCompiled()) {
//            throw new GdxRuntimeException(program.getLog());
//        }
//
//        return program;
//    }
//
//    private String[] extractShaders(String shaderCode) {
//        int fragmentStart = shaderCode.indexOf("#elif defined(FRAGMENT)"); // may also be #ifdef FRAGMENT.
//        return new String[]{ shaderCode.substring(0, fragmentStart), shaderCode.substring(fragmentStart)};
//    }
}
