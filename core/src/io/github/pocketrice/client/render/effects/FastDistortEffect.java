package io.github.pocketrice.client.render.effects;

import com.badlogic.gdx.Gdx;
import com.crashinvaders.vfx.VfxRenderContext;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.ShaderVfxEffect;
import com.crashinvaders.vfx.framebuffer.VfxFrameBuffer;
import com.crashinvaders.vfx.framebuffer.VfxPingPongWrapper;
import com.crashinvaders.vfx.gl.VfxGLUtils;
import lombok.Getter;

public class FastDistortEffect extends ShaderVfxEffect implements ChainVfxEffect {
    private static final String U_TEXTURE0 = "u_texture0";
    private static final String U_TIME = "u_time";

    @Getter
    private float time = 0f;

    public FastDistortEffect() {
        super(VfxGLUtils.compileShader(Gdx.files.classpath("gdxvfx/shaders/screenspace.vert"), Gdx.files.internal("shaders/fastdistort.frag")));
        rebind();
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        setTime(this.time + delta);
    }



    public void setTime(float time) {
        this.time = time;
        setUniform(U_TIME, time);
    }

    @Override
    public void rebind() {
        super.rebind();
        program.bind();
        program.setUniformi(U_TEXTURE0, TEXTURE_HANDLE0);
        program.setUniformf(U_TIME, time);
    }

    @Override
    public void render(VfxRenderContext context, VfxPingPongWrapper buffers) {
        render(context, buffers.getSrcBuffer(), buffers.getDstBuffer());
    }

    public void render(VfxRenderContext context, VfxFrameBuffer src, VfxFrameBuffer dst) {
        // Bind src buffer's texture as a primary one.
        src.getTexture().bind(TEXTURE_HANDLE0);
        // Apply shader effect and render result to dst buffer.
        renderShader(context, dst);
    }
}
