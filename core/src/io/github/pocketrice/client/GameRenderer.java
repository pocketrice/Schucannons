package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.ScreenUtils;
import com.crashinvaders.vfx.VfxManager;
import com.crashinvaders.vfx.effects.ChainVfxEffect;
import com.crashinvaders.vfx.effects.FilmGrainEffect;
import io.github.pocketrice.client.postproc.BlurPostProcessor;
import io.github.pocketrice.client.postproc.effects.AsciiEffect;
import io.github.pocketrice.client.postproc.effects.FastDistortEffect;
import io.github.pocketrice.client.postproc.effects.HalftoneEffect;
import io.github.pocketrice.client.ui.HUD;
import io.github.pocketrice.shared.Interlerper;
import lombok.Getter;
import lombok.Setter;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import org.javatuples.Triplet;

import java.util.UUID;

import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);

    ModelGroup cannonA, cannonB;
    ModelInstance envMi, projMi, skyMi;
    ModelBatch modelBatch;

    @Getter
    SpriteBatch postBatch;
    BlurPostProcessor postprocBlur;
    FrameBuffer fbo;
    @Getter
    VfxManager vfxManager;
    FilmGrainEffect vfxFilmGrain;
    FastDistortEffect vfxFastDistort;
    HalftoneEffect vfxHalftone;
    AsciiEffect vfxAscii;


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
    SchuAssetManager amgr;
    Audiobox audiobox;
    Fontbook fontbook;

    boolean isPaused, isPauseFirstPass, isEffectsUpdated, isAssetsLoaded;
    @Getter @Setter
    boolean isPromptBlur;


    public GameRenderer(GameManager gm) {
        amgr = gm.getAmgr();
        gmgr = gm;
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();

        modelBatch = new ModelBatch();
        envMi = new ModelInstance(amgr.aliasedGet("modelSky", Model.class));
        envMi.transform.scl(2f);
        envMi.transform.rotate(new Quaternion(Vector3.Z, (float) (Math.PI * 2)));
        projMi = new ModelInstance(amgr.aliasedGet("modelCannonWheel", Model.class));

        cannonA = new ModelGroup();
        cannonA.setGroupName("cannonA");
        cannonA.addSubmodel(amgr.aliasedGet("modelCannonBarrel", Model.class), Vector3.Zero, new Quaternion());
        cannonA.addSubmodel(amgr.aliasedGet("modelCannonWheel", Model.class), new Vector3(0f, -0.05f, 0.05f), new Quaternion());
        cannonA.addSubmodel(amgr.aliasedGet("modelCannonWheel", Model.class), new Vector3( 0f, -0.05f, -0.05f)/*new Vector3(0.1f, -0.05f, 0.1f)*/, new Quaternion(Vector3.Y, (float) (Math.PI * 2)));
        cannonA.applyOffsets();
        // cannonA.scl(5f); // tip: getTranslation is not distance from that particular vec3... it instead stores it in the passed-in vec. Oups! 1 hour debugging.

        cannonB = cannonA.cpy();
        cannonB.setGroupName("cannonB");
        //projMi.transform.scl(10f);

        postBatch = new SpriteBatch();
        postprocBlur = new BlurPostProcessor(15, 4f, 0.3f, postBatch);
        vfxManager = new VfxManager(RGBA8888);
        vfxManager.setBlendingEnabled(true);
        vfxFilmGrain = new FilmGrainEffect();
        vfxFastDistort = new FastDistortEffect();
        vfxAscii = new AsciiEffect();
        vfxHalftone = new HalftoneEffect();
        vfxManager.addEffect(vfxFilmGrain, 0);
        vfxManager.addEffect(vfxFastDistort, 1);
        vfxManager.addEffect(vfxAscii, 2);
        vfxManager.addEffect(vfxHalftone, 3);

        hud = new HUD(gmgr, this);
        gameCam = new PerspectiveCamera(80, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        gameCam.position.set(CAMERA_POS);
        gameCam.lookAt(CAMERA_LOOK);
        gameCam.near = 0.3f;
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
                        Gdx.graphics.setSystemCursor(SystemCursor.Arrow); // To avoid memory cost (of running a tad of code...). Note that cursor is set to NONE if debug is enabled so this is overwritten.
                    }

                    case Input.Keys.SHIFT_LEFT ->  {
                        SchuGame game = gmgr.getGame();
                        if (game.isDebug) {
                            inputMult.removeProcessor(inputCic);
                        }
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
                switch (keycode) {
                    case Input.Keys.SHIFT_LEFT -> {
                        SchuGame game = gmgr.getGame();
                        if (game.isDebug) {
                            inputMult.addProcessor(1, inputCic);
                        }
                    }
                }
                downKeys.remove(keycode);
                return true;
            }

            private void onMultipleKeysDown(int mostRecentKeycode) {
            }
        };

        inputMult = new InputMultiplexer(hud.getStage(), inputCic, inputKbAdapter);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 1.5f));
        env.add(new DirectionalLight().set(0.5f, 0.4f, 0.55f, 1f, 0.3f, 0f));
        env.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, 0f, 0.5f, 0f));

        isPaused = false;
        isPromptBlur = false;
        isEffectsUpdated = false;
    }



    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gameCam.update();
        inputCic.update();

        if (isPaused) {
            fbo.begin();
            renderScene();
            fbo.end();

            vfxHalftone.setSegs(140f);
            vfxHalftone.setOpacity(0.2f);
            setVfx(vfxManager, vfxFilmGrain, vfxFastDistort, vfxHalftone, vfxAscii);
            vfxManager.update(Gdx.graphics.getDeltaTime());
            vfxManager.cleanUpBuffers();
            vfxManager.beginInputCapture();

            postBatch.begin();
            postBatch.draw(fbo.getColorBufferTexture(), 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();

            vfxManager.endInputCapture();
            vfxManager.applyEffects();
            vfxManager.renderToScreen();

        }
        else if (isPromptBlur) {
            fbo.begin();
            renderScene();
            fbo.end();

            Texture ppBlur = postprocBlur.render(fbo);
            vfxHalftone.setSegs(70f);
            vfxHalftone.setOpacity(0.1f);
            setVfx(vfxManager, vfxHalftone);
            vfxManager.update(Gdx.graphics.getDeltaTime());
            vfxManager.cleanUpBuffers();
            vfxManager.beginInputCapture();

            postBatch.begin();
            postBatch.draw(ppBlur, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 0, 0, 1, 1);
            postBatch.end();

            vfxManager.endInputCapture();
            vfxManager.applyEffects();
            vfxManager.renderToScreen();
        }
//        else if (gmgr.getGame().isDebug){
//            fbo.begin();
//            renderScene();
//            fbo.end();
//
//            TextureData bufferData = fbo.getColorBufferTexture().getTextureData();
//            bufferData.prepare();
//            Pixmap bufferPix = bufferData.consumePixmap();
//            Color pickColor = new Color();
//            Color.rgba8888ToColor(pickColor, bufferPix.getPixel(Gdx.input.getX(), Gdx.input.getY()));
//
//            postBatch.begin();
//
//        }
        else {
            renderScene();
        }
    }

    private void renderScene() {
        ScreenUtils.clear(Color.valueOf("#4d4a71"), true);
        modelBatch.begin(gameCam);
        modelBatch.render(envMi, env);
        cannonA.getSubmodel(0).transform.setFromEulerAngles(0, 0, -hud.getTheta());
        gmgr.getClient().getSelf().setProjVector(sphericalToRect(hud.getMag(), degToRad(hud.getTheta()), Math.PI / 2f));
        cannonA.render(modelBatch);
        cannonB.getSubmodel(0).transform.setFromEulerAngles(0, 0, -hud.getTheta());
        cannonB.render(modelBatch);
        modelBatch.render(projMi, env);
        modelBatch.end();
        hud.render();
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

    public static Triplet<Float, Double, Double> rectToSpherical(Vector3 rectVec) {
        float rho = rectVec.len(); // does not need to be 3d b/c len is always same
        double theta = Math.atan(rectVec.y / rectVec.x);
        double phi = Math.acos(rectVec.z / rho);
        return Triplet.with(rho, theta, phi);
    }

    public static Vector2 polarToRect(float r, double theta) {
        return new Vector2((float) (r * Math.cos(theta)), (float) (r * Math.sin(theta)));
    }

    public static Vector3 sphericalToRect(float rho, double theta, double phi) {
        float x = (float) (rho * Math.sin(phi) * Math.cos(theta));
        float y = (float) (rho * Math.sin(phi) * Math.sin(theta));
        float z = (float) (rho * Math.cos(phi));

        return new Vector3(x,y,z);
    }

    public static double radToDeg(double radAng) {
        return radAng * 180 / Math.PI;
    }

    public static double degToRad(double degAng) {
        return degAng * Math.PI / 180;
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

        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        frameBufferBuilder.addBasicColorTextureAttachment(RGBA8888);

        // Enhanced precision, only needed for 3D scenes
        frameBufferBuilder.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT24);
        fbo = frameBufferBuilder.build();

        postprocBlur.buildFBO(width, height);
    }

    public static VfxManager setVfx(VfxManager vfm, ChainVfxEffect... effects) {
        vfm.removeAllEffects();
        for (ChainVfxEffect eff : effects) {
            vfm.addEffect(eff);
        }

        return vfm;
    }

    public static Model loadModel(String filepath) {
        Model res;
        FileHandle filehandle = Gdx.files.internal(filepath);
        String[] handleStrs = filepath.split("\\.");

        switch (handleStrs[handleStrs.length - 1].toLowerCase()) {
            case "gltf" -> res = new GLTFLoader().load(filehandle).scene.model;

            case "glb" -> res = new GLBLoader().load(filehandle).scene.model;

            case "obj" -> res = new ObjLoader().loadModel(filehandle);

            case "g3db" -> res = new G3dModelLoader(new JsonReader()).loadModel(filehandle);

            default -> res = null;
        }

        return res;
    }
}
