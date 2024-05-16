package io.github.pocketrice.client;

import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.SpotLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Disposable;
import io.github.pocketrice.shared.EvictingMap;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.FogAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.SpotLightEx;
import net.mgsx.gltf.scene3d.scene.*;
import net.mgsx.gltf.scene3d.utils.EnvironmentUtil;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.*;

public class GameScene { // A wrapper class for gdx-gltf's various scene objects — to make it as easy as ModelBatch.
    SceneManager ggMgr;
    Cubemap envCubemap, diffCubemap, specCubemap;
    CascadeShadowMap csm;
    SceneSkybox ggSkybox;
    GLTFLoader gltfLoader;
    EvictingMap<String, Scene> ggScenes;
    List<SpotLight> ggLights;
    Set<Disposable> ggDispose; // for garbage collection

    public GameScene() {
        ggMgr = new SceneManager();
        gltfLoader = new GLTFLoader();
        ggScenes = new EvictingMap<>(99);
        ggLights = new ArrayList<>();
        ggDispose = new HashSet<>();
    }

    public GameScene(Scene... scenes) {
        this();
        for (Scene sc : scenes) {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        }
    }

    public GameScene(SceneAsset... sceneAssets) {
        this();
        List<Scene> scs = Arrays.stream(sceneAssets).map(GameScene::extractScene).toList();

        ggDispose.addAll(List.of(sceneAssets));
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public GameScene(FileHandle... scenePaths) {
        this();
        List<SceneAsset> sas = Arrays.stream(scenePaths).map(GameScene::extractSceneAsset).toList();
        List<Scene> scs = sas.stream().map(GameScene::extractScene).toList();
        ggDispose.addAll(sas);
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void add(Model... models) {
        List<Scene> scs = Arrays.stream(models).map(GameScene::extractScene).toList();
        ggDispose.addAll(List.of(models));
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void add(ModelInstance... mis) {
        List<Scene> scs = Arrays.stream(mis).map(GameScene::extractScene).toList();
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void add(SceneAsset... sas) {
        List<Scene> scs = Arrays.stream(sas).map(GameScene::extractScene).toList();
        ggDispose.addAll(List.of(sas));
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void add(SceneGroup... sgs) {
        ggDispose.addAll(List.of(sgs));
        List<Scene> scs = Arrays.stream(sgs).map(sg -> sg.subscenes).flatMap(Collection::stream).toList(); // ALL scenes from each group
        scs.forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void add(Scene... scs) {
        Arrays.stream(scs).forEach(sc -> {
            ggScenes.put("u" + ggScenes.size(), sc);
            ggMgr.addScene(sc);
        });
    }

    public void setCubemaps(String envTexExt, String diffTexExt, String specTexExt, FileHandle brdfLut) {
        InternalFileHandleResolver ifhr = new InternalFileHandleResolver();
        envCubemap = EnvironmentUtil.createCubemap(ifhr, envTexExt, ".png", EnvironmentUtil.FACE_NAMES_NEG_POS);
        diffCubemap = EnvironmentUtil.createCubemap(ifhr,
                diffTexExt, ".png", EnvironmentUtil.FACE_NAMES_NEG_POS);
        specCubemap = EnvironmentUtil.createCubemap(ifhr,
                specTexExt, "_", ".png", 10, EnvironmentUtil.FACE_NAMES_NEG_POS);
        ggSkybox = new SceneSkybox(envCubemap);

        ggMgr.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, new Texture(brdfLut)));
        ggMgr.environment.set(PBRCubemapAttribute.createSpecularEnv(specCubemap));
        ggMgr.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffCubemap));
        ggMgr.setSkyBox(ggSkybox);
    }

    public void setCubemaps() {
        DirectionalLight light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);

        IBLBuilder iblBuilder = IBLBuilder.createOutdoor(light);
        envCubemap = iblBuilder.buildEnvMap(1024);
        diffCubemap = iblBuilder.buildIrradianceMap(256);
        specCubemap = iblBuilder.buildRadianceMap(10);
      //  ggMgr.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, new Texture(brdfLut)));
        ggMgr.environment.set(PBRCubemapAttribute.createSpecularEnv(specCubemap));
        ggMgr.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffCubemap));
        ggMgr.setSkyBox(ggSkybox);
    }

    public void testCubemaps() {
        InternalFileHandleResolver ifhr = new InternalFileHandleResolver();
        envCubemap = EnvironmentUtil.createCubemap(ifhr, "textures/skybox/env/hdr_env_", ".tga", EnvironmentUtil.FACE_NAMES_NEG_POS);
        diffCubemap = EnvironmentUtil.createCubemap(ifhr, "textures/skybox/rad/hdr_rad_", ".tga", EnvironmentUtil.FACE_NAMES_NEG_POS);
        specCubemap = EnvironmentUtil.createCubemap(ifhr, "textures/skybox/irrad/hdr_irrad_", ".tga", EnvironmentUtil.FACE_NAMES_NEG_POS);

        ggMgr.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, new Texture(ifhr.resolve("textures/skybox/brdflut.png"))));
        ggMgr.environment.set(PBRCubemapAttribute.createSpecularEnv(specCubemap));
        ggMgr.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffCubemap));
        ggMgr.setSkyBox(new SceneSkybox(envCubemap));
    }

    public void setLights() {
//        SpotLight l1 = new SpotLightEx();
//        l1.exponent = 2f;
//        l1.setPosition(new Vector3(1f, 3f, 1f));
//        l1.direction.set(3, -3, 1).nor();
//        l1.color.set(Color.valueOf("#8580f8"));

        SpotLight l2 = new SpotLightEx();
        l2.exponent = 5f;
        l2.intensity = 2f;
        l2.direction.set(-1, 3, 1).nor();
        l2.color.set(Color.valueOf("#f3c090"));

        //DirectionalShadowLight l3 = new DirectionalShadowLight(200, 200, VIEWPORT_WIDTH, VIEWPORT_HEIGHT, 0.1f, 300f);
        csm = new CascadeShadowMap(5);
        SpotLight l3 = new SpotLightEx();
        l3.color.set(Color.valueOf("#8580f8"));
        l3.direction.set(4, -3f, 4);


        //ggMgr.environment.add(l1);
        ggMgr.environment.add(l2);
        ggMgr.environment.add(l3);
        ggMgr.environment.set(new FogAttribute(FogAttribute.FogEquation).set(0.3f, 300f, 0.9f));
        ggMgr.setCascadeShadowMap(csm);
        ggMgr.setAmbientLight(0.4f);
    }

    public void addLight(Color col, Vector3 pos, Vector3 rot) {
        SpotLight l1 = new SpotLightEx();
        l1.setColor(col);
        l1.setPosition(pos);
        l1.setDirection(rot);
        addLight(l1);
    }

    public void addLight(SpotLight light) {
        ggMgr.environment.add(light);
        ggLights.add(light);
    }

    public SpotLight getLight(Vector3 pos, Vector3 dir) {
        List<SpotLight> lights = ggLights.stream().filter(l -> l.position.equals(pos) && l.direction.equals(dir)).toList();

        System.out.println(lights);
        if (lights.size() > 1) System.err.println("Violation of: several lights found from get");
        return (lights.isEmpty()) ? null : lights.get(0);
    }

    public void removeLight(SpotLight light) {
        ggLights.remove(light);
        ggMgr.environment.remove(light);
    }

    public void removeLight(Vector3 pos, Vector3 dir) {
        removeLight(getLight(pos, dir));
    }

    public void setCam(Camera cam) {
        ggMgr.setCamera(cam);
    }

    public void render(float delta) {
        //csm.setCascades(ggMgr.camera, ggMgr.getFirstDirectionalShadowLight(), 1000f, 4f);
        ggMgr.update(delta);
        ggMgr.render();
    }

    public void dispose() {
        ggMgr.dispose();
        ggDispose.forEach(Disposable::dispose);
        gltfLoader.dispose();
        envCubemap.dispose();
        diffCubemap.dispose();
        specCubemap.dispose();
        ggSkybox.dispose();
    }

    public static SceneAsset extractSceneAsset(Object cand) {
        SceneAsset sa = null;

        if (cand instanceof FileHandle fh) {
            sa = new GLTFLoader().load(fh);
        }
        else if (cand instanceof Model m) {
            SceneModel sm = new SceneModel();
            sm.model = m;

            sa = new SceneAsset();
            sa.scene = sm;
        }
        else if (cand instanceof SceneModel sm) {
            sa = new SceneAsset();
            sa.scene = sm;
        }
        else {
            System.err.println("Scene asset candidate not FileHandle, Model, or SceneModel");
        }

        return sa;
    }

    public static Scene extractScene(Object cand) {
        Scene sc = null;

        if (cand instanceof FileHandle fh) {
            sc = new Scene(extractSceneAsset(fh).scene);
        }
        else if (cand instanceof Model m) {
            sc = new Scene(extractSceneAsset(m).scene);
        }
        else if (cand instanceof ModelInstance mi) {
            sc = new Scene(mi);
        }
        else if (cand instanceof SceneModel sm) {
            sc = new Scene(sm);
        }
        else if (cand instanceof SceneAsset sa) {
            sc = new Scene(sa.scene);
        }
        else {
            System.err.println("Scene candidate not FileHandle, Model, ModelInstance, SceneModel, or SceneAsset");
        }

        return sc;
    }
}
