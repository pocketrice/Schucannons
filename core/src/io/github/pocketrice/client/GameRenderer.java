package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.ui.HUD;
import lombok.Getter;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import java.util.UUID;

import static io.github.pocketrice.client.AnsiCode.ANSI_BLUE;
import static io.github.pocketrice.client.AnsiCode.ANSI_RESET;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;
import static io.github.pocketrice.client.screens.MenuScreen.loadModel;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    ModelInstance envMi, projMi, skyMi, cannonA, cannonB;
    ModelBatch batch;
    Camera gameCam;
    @Getter
    CameraInputController gameCic;
    HUD hud;
    Environment env;
    GameManager gmgr;

    public static final Model ENV_MODEL = new GLBLoader().load(Gdx.files.internal("models/terrain.glb")).scene.model;
    public static final Model SKY_MODEL = loadModel(Gdx.files.internal("models/skypano.obj"));
    public static final Model PROJ_MODEL = new GLTFLoader().load(Gdx.files.internal("models/cannonball.gltf")).scene.model;
    public static final Model CANNON_MODEL = new GLBLoader().load(Gdx.files.internal("models/schucannon.glb")).scene.model;
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);


    public GameRenderer(GameManager gm) {
        batch = new ModelBatch();
        envMi = new ModelInstance(SKY_MODEL); // TEMP - need to optimise this to load on loading screen.
        envMi.transform.scl(2f);
        projMi = new ModelInstance(PROJ_MODEL);
        projMi.transform.scl(10f);

        cannonA = new ModelInstance(CANNON_MODEL);
        cannonA.transform.scl(5f);
        cannonB = new ModelInstance(CANNON_MODEL);
        cannonA.transform.scl(5f);

        gmgr = gm;
        hud = new HUD(gmgr);
        gameCam = new PerspectiveCamera(80, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        gameCam.position.set(CAMERA_POS);
        gameCam.lookAt(CAMERA_LOOK);
        gameCam.near = 1f;
        gameCam.far = 300f;
        gameCam.update();
        gameCic = new CameraInputController(gameCam);
        Gdx.input.setInputProcessor(gameCic);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 1.5f));
        env.add(new DirectionalLight().set(0.5f, 0.4f, 0.55f, 1f, 0.3f, 0f));
        env.add(new DirectionalLight().set(0.4f, 0.4f, 0.4f, 0f, 0.5f, 0f));
    }

    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        gameCam.update();
        gameCic.update();
        batch.begin(gameCam);
        batch.render(envMi, env);
        batch.render(cannonA, env);
        batch.render(cannonB, env);
        batch.render(projMi, env);
        batch.end();
        hud.render();
    }

    public void transformModel(ModelInstance playerModel, UUID pid) {
        Vector3[] pstate = gmgr.retrievePlayerState(pid);
        playerModel.transform.setTranslation(pstate[0]);

        // The cannon must a) have its barrel rotated on Y axis and b) entirety rotated on Z axis.
        // Thus, spherical coords are needed.
        float rho = pstate[1].len(); // ρ^2 = x^2 + y^2 + z^2 = vec3.len()
        float theta = (float) Math.atan(pstate[1].y / pstate[1].x); // tan(θ) = y/x
        float phi = (float) Math.acos(pstate[1].z / rho); // cos(φ) = z / ρ

        MeshPart schucBarrel = playerModel.model.meshParts.get(2);
        playerModel.model.meshParts.set(2, rotMeshPart(schucBarrel, new Quaternion(Vector3.Y, (float) (Math.PI - phi)))); // Replace barrel meshpart with rotated meshpart (π - φ for complement b/c cannon defaults to laying horizontally).
        playerModel.transform.rotate(new Quaternion(Vector3.Z, theta));
    }

    public void update() {
        Match matchState = gmgr.getMatchState();
        System.out.println(ANSI_BLUE + matchState.toString() + ANSI_RESET);
        //System.out.println(cannonA.model.meshParts.get(0).size + " " + cannonA.model.meshParts.get(1).size + " " + cannonA.model.meshParts.get(2).size);
        if (matchState.getCurrentPlayer() != null) {
            transformModel(cannonA, matchState.getCurrentPlayer().getPlayerId());
        }

        if (matchState.getOppoPlayer() != null) {
            transformModel(cannonB, matchState.getOppoPlayer().getPlayerId());
        }
    }

    public void dispose() {
        batch.dispose();
        hud.dispose();
    }

    public static MeshPart rotMeshPart(MeshPart mp, Quaternion rot) {
        float[] verts = new float[mp.mesh.getMaxVertices()];
        mp.mesh.getVertices(verts);

        for (int i = 0; i < verts.length; i += 3) {
            Vector3 newVert = new Vector3(verts[i], verts[i + 1], verts[i + 2]);
            newVert.mul(rot);

            verts[i] = newVert.x;
            verts[i + 1] = newVert.y;
            verts[i + 2] = newVert.z;
        }

        mp.mesh.setVertices(verts);
        return mp;
    }
}
