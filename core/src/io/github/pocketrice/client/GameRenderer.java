package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.Cursor.SystemCursor;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy;
import com.badlogic.gdx.graphics.g3d.decals.Decal;
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.Matrix4;
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
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Interlerper;
import io.github.pocketrice.shared.Interval;
import io.github.pocketrice.shared.Orientation;
import lombok.Getter;
import lombok.Setter;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.util.*;

import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;
import static io.github.pocketrice.client.Match.truncVec;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);
    public static final float CHAIN_TAP_SEC = 0.75f, DEBUG_PERSP_CULL_DIST = 0.5f;

    ModelGroup cannonA, cannonB;
    ModelInstance envMi, projMi, skyMi;
    ModelBatch modelBatch;

    @Getter
    SpriteBatch postBatch, bbBatch;
    BlurPostProcessor postprocBlur;
    FrameBuffer fbo, dpFbo;
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

    ArrayDeque<Pair<Vector3, Vector3>> debugPersps; // Refer to look+loc as perspectives, and singles as points.
    Interlerper<Vector3> dpLocInterlerp, dpLookInterlerp;
    Interval dpExitInterv;
    ModelBuilder dpBuilder;
    MeshPartBuilder dpPartBuilder;
    DecalBatch dpBatch;
    int dpRenderType;

    boolean isPaused, isPauseFirstPass, isEffectsUpdated, isDebugInterp;
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
        gameCam.near = 0.1f;
        gameCam.far = 500f;
        gameCam.update();
        inputCic = new CameraInputController(gameCam);
        inputKbAdapter = new InputAdapter() {
            final IntSet downKeys = new IntSet(20);
            final Interlerper<Float> interlerpDebugOpacity = new Interlerper<>(1f, 0f);

            @Override
            public boolean keyDown(int keycode) {
                switch (keycode) {
                    case Keys.F3 -> {
                        SchuGame game = SchuGame.globalGame();
                        game.setDebug(!game.isDebug);
                        Gdx.graphics.setSystemCursor(SystemCursor.Arrow); // To avoid memory cost (of running a tad of code...). Note that cursor is set to NONE if debug is enabled so this is overwritten.
                    }

                    case Keys.SHIFT_LEFT ->  {
                        SchuGame game = SchuGame.globalGame();
                        if (game.isDebug) {
                            inputMult.removeProcessor(inputCic);
                        }
                    }

                    case Keys.ESCAPE -> {
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

                    case Keys.TAB -> {
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
                    case Keys.SHIFT_LEFT -> {
                        SchuGame game = SchuGame.globalGame();
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
        isDebugInterp = false;

        //dpBatch = new DecalBatch(100, new CameraGroupStrategy(gameCam));
        debugPersps = new ArrayDeque<>();
        dpExitInterv = new Interval(CHAIN_TAP_SEC);
        dpRenderType = DebugPerspRenderType.NONE_TOTAL;
        dpBuilder = new ModelBuilder();
        bbBatch = new SpriteBatch();
        dpBatch = new DecalBatch(128, new CameraGroupStrategy(gameCam));
        dpFbo = new FrameBuffer(Format.RGBA8888, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, false);
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

        if (SchuGame.globalGame().isDebug()) {
            // ## Do what the keys dictate... ##
            if (Gdx.input.isKeyJustPressed(Keys.ENTER)) { // ** Create a new DP (debug perspective)
                debugPersps.add(Pair.with(gameCam.position.cpy(), gameCam.direction.cpy()));
                audiobox.playSfx("wpn_moveselect", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.LEFT_BRACKET)) { // ** Teleport to prev DP
                cycleDebugPersp(false);
                audiobox.playSfx("m3_hormenu", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.RIGHT_BRACKET)) { // ** Teleport to next DP
                cycleDebugPersp(true);
                audiobox.playSfx("m3_vertmenu", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.SEMICOLON)) { // ** Toggle debug interp
                isDebugInterp = !isDebugInterp;
                audiobox.playSfx("shell1", 1f);
                audiobox.playSfx("shell2", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.BACKSLASH)) { // ** Change render type
                dpRenderType = DebugPerspRenderType.get(dpRenderType + 1);
                audiobox.playSfx("wpn_select", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.BACKSPACE)) { // ** Remove current DP, or if chain 2x remove all DP
                if (dpExitInterv.observe()) {
                    debugPersps.clear();
                    audiobox.playSfx("unitismovingin", 1f);
                } else {
                    dpExitInterv.stamp();
                    debugPersps.remove();
                    audiobox.playSfx("unitisinbound", 1f);
                }
            } else if (Gdx.input.isKeyJustPressed(Keys.APOSTROPHE)) { // ** Teleport to nearest DP
                if (!debugPersps.isEmpty()) {
                    Pair<Vector3, Vector3> nearestDp = debugPersps.stream().min(Comparator.comparing(p -> p.getValue0().len2())).get();
                    Pair<Vector3, Vector3> candDp;
                    do { // Cycle deque until nearest reached.
                        candDp = debugPersps.poll();
                        debugPersps.add(candDp);
                    } while (!nearestDp.equals(candDp));

                    cycleDebugPersp(true);
                    audiobox.playSfx("m3_vertmenu", 1f);
                }
            }


            // ## Render the actual thing the keys have done! ##
            if (dpLocInterlerp != null && dpLocInterlerp.remainingSteps() > 0) { // INTERP CAM (DEBUG). Can safely assume lookInterlerp is also readied — this effectively replaces a redundant "isDebugInterlerp" bool. Confusingly that is already used for what settings debug should use. Ah.
                gameCam.position.set(dpLocInterlerp.advance());
                gameCam.direction.set(dpLookInterlerp.advance());
            }

            List<ModelInstance> dpMi = new ArrayList<>();

            if (dpRenderType >= DebugPerspRenderType.NONE_TOTAL) { // ...draw points
                int partIndex = 0;
                dpBuilder.begin();
                for (Pair<Vector3, Vector3> persp : debugPersps) {
                    if (gameCam.position.dst(persp.getValue0()) > DEBUG_PERSP_CULL_DIST) {
                        dpPartBuilder = dpBuilder.part("dp" + partIndex, 1, 3, new Material());
                        dpPartBuilder.setColor(Color.valueOf("#B9C4BC"));
                        dpPartBuilder.setVertexTransform(new Matrix4().translate(persp.getValue0()));
                        SphereShapeBuilder.build(dpPartBuilder, 0.5f, 0.5f, 0.5f, 4, 4);
                    }
                    partIndex++;
                }

                dpMi.add(new ModelInstance(dpBuilder.end()));
            }

            if (dpRenderType >= DebugPerspRenderType.NONE_CON) { // ...draw lines b/w points
                List<Vector3> dpList = debugPersps.stream().map(Pair::getValue0).toList();

                dpBuilder.begin();
                for (int i = 0; i < dpList.size() - 1; i++) {
                    Vector3 endA = dpList.get(i);
                    Vector3 endB = dpList.get(i + 1);

                    dpPartBuilder = dpBuilder.part("bline" + i + "-" + (i + 1), 1, 3, new Material());
                    dpPartBuilder.setColor(Color.valueOf("#AEC3B07F"));
                    dpPartBuilder.line(endA, endB);
                }

                dpMi.add(new ModelInstance(dpBuilder.end()));
            }

            if (dpRenderType >= DebugPerspRenderType.SIMPLE) { // ...draw billboards
                // Adapted from https://stackoverflow.com/questions/24375179/libgdx-decal-dynamic-text

//                for (Pair<Vector3, Vector3> persp : debugPersps) {
//                    Vector3 perspPos = persp.getValue0();
//                    Matrix4 bbProject = new Matrix4(gameCam.combined);
//                    Matrix4 bbTransform = new Matrix4().scale(0.01f, 0.01f, 1f);
//                    Vector3 bbPos = new Vector3(perspPos.x, perspPos.y - 1, perspPos.z);
//                    bbTransform.setToTranslation(bbPos);
//                    bbTransform.rotateTowardDirection(new Vector3(gameCam.direction).nor(), Vector3.Y);
//
//                    bbBatch.setProjectionMatrix(bbProject);
//                    bbBatch.setTransformMatrix(bbTransform);
                Vector3 perspPos = debugPersps.getFirst().getValue0();
//                    BitmapFont bmf = fontbook.getSizedBitmap("koholint", 25, Color.valueOf("#B9C4BC8F"));
                fontbook.font("koholint").fontSize(50).fontColor(Color.valueOf("#b9c4bc8f")).padX(250f);
                dpFbo.begin();
                Gdx.gl.glClearColor(1f, 1f, 1f, 1f);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


                bbBatch.begin();
                fontbook.formatDraw(truncVec(perspPos, 2), Orientation.LEFT, bbBatch);
                bbBatch.end();
                dpFbo.end();

                Texture dpTexture = dpFbo.getColorBufferTexture(); //replaceTexColor(dpFbo.getColorBufferTexture(), Color.BLACK, Color.CLEAR); // BUG! The FBO returns a non-alpha texture (background = black) no matter what I do — I've tried GL.blend, yes it is RGBA and not RGB, etc... so to improvise all black pixels are just cleared.
                dpTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                TextureRegion dpTexReg = new TextureRegion(dpTexture);
                dpTexReg.flip(false, true);
                Decal decal = Decal.newDecal(dpTexReg);
                decal.setPosition(new Vector3(perspPos.x, perspPos.y - 0.7f, perspPos.z));
                decal.setScale(0.01f);
                decal.lookAt(gameCam.position, gameCam.up);
                dpBatch.add(decal);
            }

            if (dpRenderType == DebugPerspRenderType.POLY) { // ...do a lil' mesh networking
                List<Vector3> dpList = debugPersps.stream().map(Pair::getValue0).toList();

                for (int i = 0; i < dpList.size(); i++) { // Draw lines to every DP (except border — the following index) for every DP.
                    Vector3 currDp = dpList.get(i);
                    Vector3 borderDp = dpList.get((i == dpList.size() - 1) ? 0 : i);

                    List<Vector3> filteredDp = dpList.stream().filter(dp -> !dp.equals(borderDp)).toList();

                    for (Vector3 fdp : filteredDp) {
                        dpBuilder.begin();
                        dpPartBuilder = dpBuilder.part("pline" + i + "-" + dpList.indexOf(fdp), 1, 3, new Material());
                        dpPartBuilder.setColor(Color.valueOf("#8096823F"));
                        dpPartBuilder.line(currDp, fdp);
                        dpMi.add(new ModelInstance(dpBuilder.end()));
                    }
                }
            }

            dpBatch.flush();
            modelBatch.begin(gameCam);
            dpMi.forEach(mi -> modelBatch.render(mi));
            modelBatch.end();
        }

        hud.render();
    }

    public void cycleDebugPersp(boolean isForward) {
        Pair<Vector3, Vector3> currPersp = Pair.with(gameCam.position.cpy(), gameCam.direction.cpy()); // Note that it doesn't have to be a persp in the list — may be any the player has changed it to!
        Pair<Vector3, Vector3> nextPersp;

        if (isForward) {
            nextPersp = debugPersps.poll(); // 12345 -> 2345[1] (next is shifted).
            debugPersps.add(nextPersp);
        } else {
            nextPersp = debugPersps.peekLast(); // 12345 -> [5]1234 (next is still 1. but needs to be shifted too)
            debugPersps.addFirst(nextPersp);
        }

        if (isDebugInterp) { // Activate smooth interp over...
            dpLocInterlerp = new Interlerper<>(currPersp.getValue0(), nextPersp.getValue0(), EasingFunction.SMOOTHERSTEP, 200);
            dpLookInterlerp = new Interlerper<>(currPersp.getValue1(), nextPersp.getValue1(), EasingFunction.SMOOTHERSTEP, 200);
        } else { // ...or just teleport over.
            gameCam.position.set(nextPersp.getValue0());
            gameCam.direction.set(nextPersp.getValue1());
        }
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

    public void buildFBOs(int width, int height) {
        if (width == 0 || height == 0) return;

        if (fbo != null) fbo.dispose();

        FrameBufferBuilder frameBufferBuilder = new FrameBufferBuilder(width, height);
        frameBufferBuilder.addBasicColorTextureAttachment(RGBA8888);

        // Enhanced precision, only needed for 3D scenes
        frameBufferBuilder.addDepthRenderBuffer(GL30.GL_DEPTH_COMPONENT24);
        fbo = frameBufferBuilder.build();

        postprocBlur.buildFBO(width, height);
    }

    public static Texture replaceTexColor(Texture tex, Color oldCol, Color newCol) {
        TextureData texData = tex.getTextureData();
        if (!texData.isPrepared()) texData.prepare();
        Pixmap pix = texData.consumePixmap();

        for (int x = 0; x < pix.getWidth(); x++) {
            for (int y = 0; y < pix.getHeight(); y++) {
                if (pix.getPixel(x,y) == oldCol.toIntBits()) {
                    pix.drawPixel(x, y, newCol.toIntBits());
                }
            }
        }

        return new Texture(pix);
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

    static class DebugPerspRenderType {
        public static final int NONE_TOTAL = 0;
        public static final int NONE_CON = 1;
        public static final int SIMPLE = 2;
        public static final int POLY = 3;

        static int get(int i) {
            int[] renderTypes = { NONE_TOTAL, NONE_CON, SIMPLE, POLY };
            int maxIndex = renderTypes.length - 1;
            int wrappedIndex = (i > maxIndex) ? 0 : (i < 0) ? maxIndex : i;
            return renderTypes[wrappedIndex];
        }
    }
}
