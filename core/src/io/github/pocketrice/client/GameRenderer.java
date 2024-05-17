package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
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
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.SphereShapeBuilder;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.GLFrameBuffer.FrameBufferBuilder;
import com.badlogic.gdx.math.*;
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
import io.github.pocketrice.shared.*;
import lombok.Getter;
import lombok.Setter;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import org.javatuples.Pair;
import org.javatuples.Triplet;

import java.io.InvalidObjectException;
import java.util.*;

import static com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888;
import static io.github.pocketrice.client.Match.truncVec;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;
import static io.github.pocketrice.client.ui.FocusableGroup.wrapIndex;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    public static final Vector3 CAMERA_POS = new Vector3(0f, 0f, 0f), CAMERA_LOOK = new Vector3(0f, 0f, 1f);
    public static final float CHAIN_TAP_SEC = 0.75f, DEBUG_PERSP_CULL_DIST = 0.5f;

    SchuGame game;
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
    SchuCameraInput inputSic;
    @Getter
    InputAdapter inputKbic;
    @Getter
    InputMultiplexer inputMult;
    HUD hud;
    Environment env;
    GameManager gmgr;
    SchuAssetManager amgr;
    Audiobox audiobox;
    Fontbook fontbook;

    ArrayDeque<Pair<Vector3, Vector3>> debugPersps; // Refer to look+loc as perspectives, and singles as points.
    EvictingMap<Vector3[], Set<ModelInstance>> dpModelCache;
    EvictingMap<Vector3, Decal> dpDecalCache;
    Interlerper<Vector3> dpLocInterlerp, dpLookInterlerp;
    Interval dpExitInterv;
    ModelBuilder dpBuilder;
    MeshPartBuilder dpPartBuilder;
    DecalBatch dpBatch;

    int dpRenderType;
    boolean isPauseFirstPass, isEffectsUpdated, isDebugInterp, isDebugFlattened; // <- as in, measuring 2d pixels now
    @Getter @Setter
    boolean isPromptBlur;



    public GameRenderer(GameManager gm) throws InvalidObjectException {
        game = SchuGame.globalGame();
        amgr = gm.getAmgr();
        gmgr = gm;
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();

        modelBatch = new ModelBatch();
        envMi = new ModelInstance(amgr.aliasedGet("modelSky", Model.class));
        envMi.transform.scl(5f);
        envMi.transform.rotate(Vector3.X, 180f);
        projMi = new ModelInstance(amgr.aliasedGet("modelCannonLeg", Model.class));
        projMi.transform.scl(0.01f);

        cannonA = new ModelGroup();
        cannonA.setGroupName("cannonA");
        cannonA.setInterp(true);
        cannonA.addSubmodel(amgr.aliasedGet("modelCannonBrl", Model.class), Vector3.Zero, new Quaternion());
        cannonA.addSubmodel(amgr.aliasedGet("modelCannonLeg", Model.class), Vector3.Zero.cpy(), new Quaternion(Vector3.Y, (float) (Math.PI * 2)));
        cannonA.applyOffsets();
        cannonA.translate(new Vector3(0f, 0, 0));
        // cannonA.scl(5f); // tip: getTranslation is not distance from that particular vec3... it instead stores it in the passed-in vec. Oups! 1 hour debugging.

        cannonB = cannonA.cpy();
        cannonB.setInterp(true);
        cannonB.setPos(new Vector3(20f, 0f, 0));
        cannonB.applyOffsets();
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

        inputSic = new SchuCameraInput(gameCam);
        inputKbic = new InputAdapter() {
            final IntSet downKeys = new IntSet(20);

            @Override
            public boolean keyDown(int keycode) {
                boolean isKeyed = true;

                switch (keycode) {
                    case Keys.F3 -> {
                        game.setDebug(!game.isDebug); // To avoid memory cost (of running a tad of code...). Note that cursor is set to NONE if debug is enabled so this is overwritten.
                        //if (!game.isDebug) inputSic.camLock(true); // stupid patchwork fix
                    }

//                    case Keys.SHIFT_RIGHT ->  {
//                        if (game.isDebug) {
//                            processDebugFlatten(!isDebugFlattened);
//                        }
//                    }

                    case Keys.ESCAPE -> {
                        game.setPaused(!game.isPaused); // Invert pause status
                        isPauseFirstPass = true;

                        if (game.isPaused) {
                            audiobox.playSfx("hitsound", 1f);
                            Gdx.input.setInputProcessor(inputKbic);
                            inputSic.camLock(false);
                        } else {
                            audiobox.playSfx("vote_started", 0.5f);
                            Gdx.input.setInputProcessor(inputMult);
                            inputSic.camLock(true);
                        }
                    }

                    case Keys.TAB -> {
                        System.out.println("plist now!11!1!");
                    }

                    default -> {
                        isKeyed = false;
                    }
                }

                // sourc'd
                downKeys.add(keycode);
                if (downKeys.size >= 2) {
                    if (!isKeyed) isKeyed = onMultipleKeysDown(keycode);
                }

                return isKeyed;
            }

//            @Override
//            public boolean keyUp(int keycode) {
//                boolean isKeyed = true;
//
//                switch (keycode) {
////                    case Keys.SHIFT_RIGHT -> {
////                        SchuGame game = SchuGame.globalGame();
////                        if (game.isDebug) {
////                            inputMult.addProcessor(2, inputSic);
////                        }
////                    }
//
//                    default -> {
//                        isKeyed = false;
//                    }
//                }
//                downKeys.remove(keycode);
//                return isKeyed;
//            }

            private void processDebugFlatten(boolean isFlattened) {
                isDebugFlattened = isFlattened;

                if (isDebugFlattened) {
                    inputSic.purge();
                    inputMult.removeProcessor(inputSic);
                } else {
                    inputMult.addProcessor(2, inputSic);
                }
            }
            private boolean onMultipleKeysDown(int mostRecentKeycode) {
                return false;
            }
        };

        inputMult = new InputMultiplexer(hud.getStage(), inputKbic, inputSic);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 0.8f));
        env.add(new DirectionalLight().set(0.4f, 0.4f, 0.55f, -1f, 0.3f, 0f));
        env.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, 0f, 0.5f, 0f));

        isPromptBlur = false;
        isEffectsUpdated = false;
        isDebugInterp = false;
        isDebugFlattened = false;

        debugPersps = new ArrayDeque<>();
        dpModelCache = new EvictingMap<>(128);
        dpDecalCache = new EvictingMap<>(128);
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
        inputSic.update();

        if (game.isPaused) {
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
       // cannonA.setSubrot(0, ModelGroup.eulerToQuat(new Vector3(0, hud.getPhi(), 0)));

        gmgr.getClient().getSelf().setProjVector(sphericalToRect(hud.getMag(), degToRad(hud.getTheta()), Math.PI / 2f));
       // cannonA.update();
       // cannonA.render(modelBatch);

        cannonB.setSubrot(0, ModelGroup.eulerToQuat(new Vector3(0, hud.getPhi(), 0)));
      //  cannonB.setSubrot(0, ModelGroup.eulerToQuat(new Vector3(hud.getTheta(), 0, 0)));
       // cannonB.setSubrot(0, new Quaternion(Vector3.Y, hud.getTheta()));
        cannonB.setPos(hud.getMov());
        cannonB.update();
        cannonB.render(modelBatch);

        modelBatch.render(projMi, env);
        modelBatch.end();

        if (SchuGame.globalGame().isDebug()) {
            // ## Do what the keys dictate... ##
            if (Gdx.input.isKeyJustPressed(Keys.ENTER)) { // ** Create a new DP (debug perspective)
                debugPersps.add(Pair.with(gameCam.position.cpy(), gameCam.direction.cpy()));
                audiobox.playSfx("wpn_moveselect", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.LEFT_BRACKET)) { // ** Teleport to prev DP
                if (!debugPersps.isEmpty()) {
                    cycleDebugPersp(false);
                }
                audiobox.playSfx("m3_hormenu", 1f);
            } else if (Gdx.input.isKeyJustPressed(Keys.RIGHT_BRACKET)) { // ** Teleport to next DP
                if (!debugPersps.isEmpty()) {
                    cycleDebugPersp(true);
                }
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
                    audiobox.playSfx("cs_plant", 1f);
                } else {
                    dpExitInterv.stamp();
                    debugPersps.poll(); // Remove from queue
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
            } else if (Gdx.input.isKeyJustPressed(Keys.R)) { // ** Reset camera
                gameCam.position.set(Vector3.Zero);
                gameCam.lookAt(Vector3.Z);
            }

            // ## Render the actual thing the keys have done! ##
            if (dpLocInterlerp != null && dpLocInterlerp.remainingSteps() > 0) { // INTERP CAM (DEBUG). Can safely assume lookInterlerp is also readied — this effectively replaces a redundant "isDebugInterlerp" bool. Confusingly that is already used for what settings debug should use. Ah.
                gameCam.position.set(dpLocInterlerp.advance());
                gameCam.direction.set(dpLookInterlerp.advance());
            }

            // list of things to render
            // add them per step
            //
            Vector3[] perspsPos = debugPersps.stream().map(Pair::getValue0).toArray(Vector3[]::new);
            dpModelCache.putIfAbsent(perspsPos, new HashSet<>());

            if (dpRenderType >= DebugPerspRenderType.NONE_TOTAL) { // ...draw points
                int partIndex = 0;

                if (dpModelCache.get(perspsPos).isEmpty()) {
                    dpBuilder.begin();

                    for (Vector3 persp : perspsPos) {
                        if (gameCam.position.dst(persp) > DEBUG_PERSP_CULL_DIST) {
                            dpPartBuilder = dpBuilder.part("dppos" + partIndex, 1, 3, new Material());
                            dpPartBuilder.setColor(Color.valueOf("#A5A2DB"));
                            dpPartBuilder.setVertexTransform(new Matrix4().translate(persp));
                            SphereShapeBuilder.build(dpPartBuilder, 0.5f, 0.5f, 0.5f, 3, 3);

//                        dpPartBuilder = dpBuilder.part("dpcam" + partIndex, 1, 3, new Material());
//                        dpPartBuilder.setColor(Color.valueOf("#B9C4BC"));
//                        dpPartBuilder.setVertexTransform(new Matrix4().setToLookAt(persp.getValue0(), persp.getValue1(), Vector3.Z));
//                        ConeShapeBuilder.build(dpPartBuilder, 0.3f, 0.6f, 0.3f, 4);
                        }
                        partIndex++;
                    }

                    dpModelCache.get(perspsPos).add(new ModelInstance(dpBuilder.end()));
                }
            }

            if (dpRenderType >= DebugPerspRenderType.NONE_CON) { // ...draw lines b/w points
                List<Vector3> dpList = debugPersps.stream().map(Pair::getValue0).toList();

                if (dpModelCache.get(perspsPos).size() == 1) {
                    dpBuilder.begin();

                    for (int i = 0; i < dpList.size() - 1; i++) {
                        Vector3 endA = dpList.get(i);
                        Vector3 endB = dpList.get(i + 1);

                        dpPartBuilder = dpBuilder.part("bline" + i + "-" + (i + 1), 1, 3, new Material());
                        dpPartBuilder.setColor(Color.valueOf("#A095CC7F"));
                        dpPartBuilder.line(endA, endB);
                    }

                    dpModelCache.get(perspsPos).add(new ModelInstance(dpBuilder.end()));
                }
            }

            if (dpRenderType >= DebugPerspRenderType.SIMPLE) { // ...draw billboards
                // Adapted from https://stackoverflow.com/questions/24375179/libgdx-decal-dynamic-text
                for (Vector3 perspPos : perspsPos) {
//                    Vector3 perspPos = persp.getValue0();
//                    Matrix4 bbProject = new Matrix4(gameCam.combined);
//                    Matrix4 bbTransform = new Matrix4().scale(0.01f, 0.01f, 1f);
//                    Vector3 bbPos = new Vector3(perspPos.x, perspPos.y - 1, perspPos.z);
//                    bbTransform.setToTranslation(bbPos);
//                    bbTransform.rotateTowardDirection(new Vector3(gameCam.direction).nor(), Vector3.Y);
//
//                    bbBatch.setProjectionMatrix(bbProject);
//                    bbBatch.setTransformMatrix(bbTransform);
                    if (!dpDecalCache.containsKey(perspPos)) {
                        fontbook.font("koholint").fontSize(50).fontColor(Color.valueOf("#A095CC")).padX(250f);
                        dpFbo.begin();
                        Gdx.gl.glClearColor(0f, 0f, 0f, 0f);
                        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

                        bbBatch.begin();
                        fontbook.formatDraw(truncVec(perspPos, 2), Orientation.LEFT, bbBatch);
                        bbBatch.end();

                        Pixmap pix = Pixmap.createFromFrameBuffer(0, 0, dpFbo.getWidth(), dpFbo.getHeight());
                        dpFbo.end();

                        Texture dpTexture = new Texture(pix); // (BUG) NO BUG! The FBO returns a non-alpha texture (background = black) no matter what I do — I've tried GL.blend, yes it is RGBA and not RGB, etc... so to improvise all black pixels are just cleared. [5 hours later] Actually, I am stupid :( it is a decal problem; to render w/ transparency an add'l bool was passed to constructor. Aw.
                        dpTexture.setFilter(TextureFilter.Linear, TextureFilter.Linear);
                        TextureRegion dpTexReg = new TextureRegion(dpTexture);
                        dpTexReg.flip(false, true);

                        Decal decal = Decal.newDecal(dpTexReg, true);
                        decal.setScale(0.01f);

                        dpDecalCache.put(perspPos, decal);
                    }

                    // Note this is outside of check since decals need to be readded per frame (due to flush)
                    Decal cachedDecal = dpDecalCache.get(perspPos);
                    cachedDecal.setPosition(new Vector3(perspPos.x, perspPos.y - 0.7f, perspPos.z)); // Update transforms
                    cachedDecal.lookAt(gameCam.position, gameCam.up);
                    dpBatch.add(cachedDecal);
                }
            }

//            if (dpRenderType == DebugPerspRenderType.LIGHTS) {
//                debugPersps.stream().map(dp -> {
//                    SpotLight sl = new SpotLightEx();
//                    sl.setPosition(dp.getValue0());
//                    sl.setDirection(dp.getValue1());
//                    return sl;
//                }).forEach(sl -> gscene.addLight(sl));
//            } else {
//               debugPersps.forEach(dp -> gscene.removeLight(dp.getValue0(), dp.getValue1()));
//            }

            if (dpRenderType == DebugPerspRenderType.POLY) { // ...do a lil' mesh networking
                List<Vector3> dpList = debugPersps.stream().map(Pair::getValue0).toList();

                if (dpModelCache.get(perspsPos).size() == 2) {
                    for (int i = 0; i < dpList.size(); i++) { // Draw lines to every DP (except border — the following index) for every DP.
                        Vector3 currDp = dpList.get(i);
                        Vector3 borderPrevDp = dpList.get(wrapIndex(i - 1, dpList.size() - 1));
                        Vector3 borderNextDp = (i == dpList.size() - 1) ? null : dpList.get(wrapIndex(i + 1, dpList.size() - 1)); // Don't cull for last one.

                        List<Vector3> filteredDp = dpList.stream().filter(dp -> !dp.equals(borderPrevDp) && !dp.equals(borderNextDp)).toList();

                        for (Vector3 fdp : filteredDp) {
                            dpBuilder.begin();
                            dpPartBuilder = dpBuilder.part("pline" + i + "-" + dpList.indexOf(fdp), 1, 3, new Material());
                            dpPartBuilder.setColor(Color.valueOf("#726EB5"));
                            dpPartBuilder.line(currDp, fdp);
                            dpModelCache.get(perspsPos).add(new ModelInstance(dpBuilder.end()));
                        }
                    }
                }
            }


            dpBatch.flush();
            modelBatch.begin(gameCam);
            dpModelCache.get(perspsPos).forEach(mi -> modelBatch.render(mi));
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
            dpLocInterlerp = new Interlerper<>(currPersp.getValue0(), nextPersp.getValue0(), EasingFunction.SMOOTHERSTEP, 150);
            dpLookInterlerp = new Interlerper<>(currPersp.getValue1(), nextPersp.getValue1(), EasingFunction.SMOOTHERSTEP, 150);
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

        playerModel.subrotate(0, new Quaternion(Vector3.Y, (float) (Math.PI - phi))); // Replace barrel meshpart with rotated meshpart (π - φ for complement b/c cannon defaults to laying horizontally).
        playerModel.rotate(new Quaternion(Vector3.X, theta));
    }

    public void transformModel(SceneGroup playerScene, UUID pid) {
        Vector3[] pstate = gmgr.retrievePlayerState(pid);
        playerScene.translate(pstate[0]);

        // The cannon must a) have its barrel rotated on Y axis and b) entirety rotated on Z axis.
        // Thus, spherical coords are needed.
        float rho = pstate[1].len(); // ρ^2 = x^2 + y^2 + z^2 = vec3.len()
        float theta = (rho == 0) ? 0 : (float) Math.atan(pstate[1].y / pstate[1].x); // tan(θ) = y/x
        float phi = (float) ((rho == 0) ? Math.PI : Math.acos(pstate[1].z / rho)); // cos(φ) = z / ρ

        playerScene.subrotate(0, new Quaternion(Vector3.Y, (float) (Math.PI - phi))); // Replace barrel meshpart with rotated meshpart (π - φ for complement b/c cannon defaults to laying horizontally).
        playerScene.rotate(new Quaternion(Vector3.Z, theta));
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
        return radAng * MathUtils.radDeg;
    }

    public static double degToRad(double degAng) {
        return degAng * MathUtils.degRad;
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

        if (game.isCursorLocked) { // Update loop implementation of SchuGame's "cursor lock" (no update loop there. I don't want 5 different loops :>)
            Gdx.input.setCursorPosition((int) game.cursorLockPos.x, (int) game.cursorLockPos.y);
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

        return new Texture(pix);
    }

    public static Pixmap replacePixColor(Pixmap pix, Color oldCol, Color newCol) {
        Pixmap pixCpy = new Pixmap(pix.getWidth(), pix.getHeight(), pix.getFormat());

        for (int x = 0; x < pix.getWidth(); x++) {
            for (int y = 0; y < pix.getHeight(); y++) {
                if (pix.getPixel(x,y) == Color.rgba8888(oldCol)) { // Notes... pix.getPixel() returns RGBA int, whereas col.toIntBits() returns ARGB int. Different bit placements, so use Color.[format] instead!
                    pixCpy.drawPixel(x, y, Color.rgba8888(newCol));
                } else {
                    pixCpy.drawPixel(x,y,pix.getPixel(x,y));
                }
            }
        }

        return pix;
    }

//    public static Texture replaceFboTexColor(Texture tex, Color oldCol, Color newCol) { // hacky inefficient alternative b/c fbo color buffer "can't be converted to pixmap"?
//        ShaderProgram alphaHackShader = buildShader("alphahack.frag");
//        FrameBuffer fbo = new FrameBuffer(Format.RGBA8888, tex.getWidth(), tex.getHeight(), false);
//        SpriteBatch batch = new SpriteBatch();
//
//        ShaderProgram.pedantic = false;
//        alphaHackShader.bind();
//        alphaHackShader.setUniformi("u_texture", 0);
//        alphaHackShader.setUniform4fv("oldColor", new float[]{oldCol.r, oldCol.g, oldCol.b, oldCol.a}, 0, 4);
//        alphaHackShader.setUniform4fv("newColor", new float[]{newCol.r, newCol.g, newCol.b, newCol.a}, 0, 4);
//        batch.setShader(alphaHackShader);
//        fbo.begin();
//        batch.begin();
//        batch.draw(new TextureRegion(tex), 0, 0);
//        batch.end();
//        fbo.end();
//
//        return fbo.getColorBufferTexture();
//    }

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
        public static final int LIGHTS = 3;
        public static final int POLY = 4;

        static int get(int i) {
            int[] renderTypes = { NONE_TOTAL, NONE_CON, SIMPLE, LIGHTS, POLY };
            int maxIndex = renderTypes.length - 1;
            int wrappedIndex = (i > maxIndex) ? 0 : (i < 0) ? maxIndex : i;
            return renderTypes[wrappedIndex];
        }
    }
}
