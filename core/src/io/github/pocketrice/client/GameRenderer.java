package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.FilmGrainEffect;
import io.github.pocketrice.client.effects.FastDistortEffect;
import io.github.pocketrice.client.ui.HUD;
import io.github.pocketrice.shared.Interlerper;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;
import static io.github.pocketrice.client.screens.MenuScreen.loadModel;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    private final Audiobox audiobox = Audiobox.of(List.of("hitsound.ogg", "duel_challenge.ogg", "vote_started.ogg"), List.of());
    ModelGroup cannonA, cannonB;
    ModelInstance envMi, projMi, skyMi;
    ModelBatch modelBatch;

    @Getter
    SpriteBatch postBatch;
    @Getter
    VfxManager vfxManager;
    FrameBuffer fbo;
    FrameBuffer blurTrgtA, blurTrgtB, distortTarget;
    ShaderProgram blurShader, defaultShader;
    @Getter
    Camera gameCam;
    @Getter
    CameraInputController inputCic;
    @Getter
    InputAdapter inputKbAdapter;
    @Getter
    InputMultiplexer inputMult;
    HUD hud;
    Environment env;
    GameManager gmgr;

    boolean isPaused, isPauseFirstPass;
    int pingPongCount; // Higher val = greater GPU cost
    float time;

    static final float MAX_BLUR = 4f;


    public static final Model ENV_MODEL = loadModel(Gdx.files.internal("models/terrain.glb"));
    public static final Model SKY_MODEL = loadModel(Gdx.files.internal("models/skypano.obj"));
    public static final Model PROJ_MODEL = loadModel(Gdx.files.internal("models/cannonball.gltf"));
    public static final Model CANNON_BARREL_MODEL = loadModel(Gdx.files.internal("models/schubarrel.obj"));
    public static final Model CANNON_WHEEL_MODEL = loadModel(Gdx.files.internal("models/schuwheel.obj"));
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);


    public GameRenderer(GameManager gm) {
        modelBatch = new ModelBatch();
        envMi = new ModelInstance(SKY_MODEL); // TEMP - need to optimise this to load on loading screen.
        envMi.transform.scl(2f);
        envMi.transform.rotate(new Quaternion(Vector3.Z, (float) (Math.PI * 2)));
        projMi = new ModelInstance(PROJ_MODEL);
        projMi.transform.scl(10f);

        postBatch = new SpriteBatch();
        vfxManager = new VfxManager(Pixmap.Format.RGBA8888);
        defaultShader = postBatch.getShader();
        blurShader = buildShader("shaders/blur.vert", "shaders/blur.frag");

        vfxManager.setBlendingEnabled(true);
        vfxManager.addEffect(new FastDistortEffect());
        vfxManager.addEffect(new FilmGrainEffect());


        cannonA = new ModelGroup();
        cannonA.setGroupName("cannonA");
        cannonA.addSubmodel(CANNON_BARREL_MODEL, Vector3.Zero, new Quaternion());
        cannonA.addSubmodel(CANNON_WHEEL_MODEL, new Vector3(0f, -0.05f, 0.05f), new Quaternion());
        cannonA.addSubmodel(CANNON_WHEEL_MODEL, new Vector3( 0f, -0.05f, -0.05f)/*new Vector3(0.1f, -0.05f, 0.1f)*/, new Quaternion(Vector3.Y, (float) (Math.PI * 2)));
        cannonA.applyOffsets();
        cannonA.scl(5f); // tip: getTranslation is not distance from that particular vec3... it instead stores it in the passed-in vec. Oups! 1 hour debugging.

        cannonB = cannonA.cpy();
        cannonB.setGroupName("cannonB");

        gmgr = gm;
        hud = new HUD(gmgr, this);
        gameCam = new PerspectiveCamera(80, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        gameCam.position.set(CAMERA_POS);
        gameCam.lookAt(CAMERA_LOOK);
        gameCam.near = 1f;
        gameCam.far = 300f;
        gameCam.update();
        inputCic = new CameraInputController(gameCam);
        inputKbAdapter = new InputAdapter() {
            final IntSet downKeys = new IntSet(20);
            final Interlerper<Float> interlerpDebugOpacity = new Interlerper<>(1f, 0f);

            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Input.Keys.F3 -> {
                        SchuGame game = gmgr.getGame();
                        game.setDebug(!game.isDebug);
                    }

                    case Input.Keys.ESCAPE -> {
                        isPaused = !isPaused;
                        isPauseFirstPass = true;

                        if (isPaused) {
                            audiobox.playSfx("hitsound", 1f);
                            Gdx.input.setInputProcessor(inputKbAdapter);
                        } else {
                            audiobox.playSfx("vote_started", 0.5f);
                            Gdx.input.setInputProcessor(inputMult);
                        }
                    }

                    case Input.Keys.TAB -> {
                        System.out.println("plist now!11!1!");
                    }
                }

                // sourc'd
                downKeys.add(keycode);
                if (downKeys.size >= 2) {
                    onMultipleKeysDown(keycode);
                }
                return true;
            }

            @Override
            public boolean keyUp(int keycode) {
                downKeys.remove(keycode);
                return true;
            }

            private void onMultipleKeysDown(int mostRecentKeycode) {
            }
        };

        inputMult = new InputMultiplexer(inputCic, inputKbAdapter);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 1.5f));
        env.add(new DirectionalLight().set(0.5f, 0.4f, 0.55f, 1f, 0.3f, 0f));
        env.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, 0f, 0.5f, 0f));

        isPaused = false;
        pingPongCount = 6;
        time = 0f;
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

    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gameCam.update();
        inputCic.update();

        time += Gdx.graphics.getDeltaTime();

        if (isPaused) {
            fbo.begin();
            renderScene();
            fbo.end();


            // Get color texture from frame buffer
            Texture fboTex = fbo.getColorBufferTexture();

            // Apply blur and retrieve texture
            Texture blurResult = postProcessTexture(fboTex);

            vfxManager.update(Gdx.graphics.getDeltaTime());
            vfxManager.cleanUpBuffers();
            vfxManager.beginInputCapture();

            postBatch.begin();
            postBatch.draw(blurResult, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();

            vfxManager.endInputCapture();
            vfxManager.applyEffects();
            vfxManager.renderToScreen();

        } else {
            renderScene();
        }

    }

    private void renderScene() {
        ScreenUtils.clear(Color.BLUE, true);
        modelBatch.begin(gameCam);
        modelBatch.render(envMi, env);
        cannonA.render(modelBatch);
        cannonB.render(modelBatch);
        modelBatch.render(projMi, env);
        modelBatch.end();
        hud.render();
    }

    private Texture postProcessTexture(Texture fboTex) {
        postBatch.setShader(blurShader);

        for (int i = 0; i < pingPongCount; i++) {
            // Horizontal blur pass
            blurTrgtA.begin();
            postBatch.begin();
            blurShader.setUniformf("dir", 0.5f, 0); // horizontal dir
            blurShader.setUniformf("radius",  MAX_BLUR);
            blurShader.setUniformf("resolution", Gdx.graphics.getWidth());
            postBatch.draw((i == 0) ? fboTex : blurTrgtB.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();
            blurTrgtA.end();

            // Vertical blur pass
            blurTrgtB.begin();
            postBatch.begin();
            blurShader.setUniformf("dir", 0, 0.5f);
            blurShader.setUniformf("radius", MAX_BLUR);
            blurShader.setUniformf("resolution", Gdx.graphics.getHeight());
            postBatch.draw(blurTrgtA.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();
            blurTrgtB.end();
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

        return blurTrgtB.getColorBufferTexture();
    }

    public void transformModel(ModelGroup playerModel, UUID pid) {
        Vector3[] pstate = gmgr.retrievePlayerState(pid);
        playerModel.translate(pstate[0]);

        // The cannon must a) have its barrel rotated on Y axis and b) entirety rotated on Z axis.
        // Thus, spherical coords are needed.
        float rho = pstate[1].len(); // ρ^2 = x^2 + y^2 + z^2 = vec3.len()
        float theta = (rho == 0) ? 0 : (float) Math.atan(pstate[1].y / pstate[1].x); // tan(θ) = y/x
        float phi = (float) ((rho == 0) ? Math.PI : Math.acos(pstate[1].z / rho)); // cos(φ) = z / ρ

        playerModel.getSubmodel(0).transform.rotate(new Quaternion(Vector3.Y, (float) (Math.PI - phi))); // Replace barrel meshpart with rotated meshpart (π - φ for complement b/c cannon defaults to laying horizontally).
        playerModel.rotate(new Quaternion(Vector3.Z, theta));
    }

    public void update() {
        Match matchState = gmgr.getMatchState();
        //System.out.println(ANSI_BLUE + matchState.toString() + ANSI_RESET);
        //System.out.println(cannonA.model.meshParts.get(0).size + " " + cannonA.model.meshParts.get(1).size + " " + cannonA.model.meshParts.get(2).size);
        if (matchState.getCurrentPlayer() != null) {
            transformModel(cannonA, matchState.getCurrentPlayer().getPlayerId());
        }

        if (matchState.getOppoPlayer() != null) {
            transformModel(cannonB, matchState.getOppoPlayer().getPlayerId());
        }
    }

    public void dispose() {
        modelBatch.dispose();
        hud.dispose();
    }

    public void buildFBO(int width, int height) {
        if (width == 0 || height == 0) return;

        if (fbo != null) fbo.dispose();
        if (blurTrgtA != null) blurTrgtA.dispose();
        if (blurTrgtB != null) blurTrgtB.dispose();

        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        frameBufferBuilder.addBasicColorTextureAttachment(Pixmap.Format.RGBA8888);

        // Enhanced precision, only needed for 3D scenes
        frameBufferBuilder.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT24);
        fbo = frameBufferBuilder.build();

        float blurScale = 1f;
        blurTrgtA = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (width * blurScale), (int) (height * blurScale), false);
        blurTrgtB = new FrameBuffer(Pixmap.Format.RGBA8888, (int) (width * blurScale), (int) (height * blurScale), false);
        distortTarget = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);
    }
}
