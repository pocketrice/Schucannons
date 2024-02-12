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
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.ui.HUD;
import lombok.Getter;

import java.util.UUID;

import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;
import static io.github.pocketrice.client.screens.MenuScreen.loadModel;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    ModelGroup cannonA, cannonB;
    ModelInstance envMi, projMi, skyMi;
    ModelBatch batch;
    @Getter
    Camera gameCam;
    @Getter
    CameraInputController gameCic;
    HUD hud;
    Environment env;
    GameManager gmgr;

    public static final Model ENV_MODEL = loadModel(Gdx.files.internal("models/terrain.glb"));
    public static final Model SKY_MODEL = loadModel(Gdx.files.internal("models/skypano.obj"));
    public static final Model PROJ_MODEL = loadModel(Gdx.files.internal("models/cannonball.gltf"));
    public static final Model CANNON_BARREL_MODEL = loadModel(Gdx.files.internal("models/schubarrel.obj"));
    public static final Model CANNON_WHEEL_MODEL = loadModel(Gdx.files.internal("models/schuwheel.obj"));
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);


    public GameRenderer(GameManager gm) {
        batch = new ModelBatch();
        envMi = new ModelInstance(SKY_MODEL); // TEMP - need to optimise this to load on loading screen.
        envMi.transform.scl(2f);
        envMi.transform.rotate(new Quaternion(Vector3.Z, (float) (Math.PI * 2)));
        projMi = new ModelInstance(PROJ_MODEL);
        projMi.transform.scl(10f);

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
        cannonA.render(batch);
        cannonB.render(batch);
        batch.render(projMi, env);
        batch.end();
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
        batch.dispose();
        hud.dispose();
    }
}
