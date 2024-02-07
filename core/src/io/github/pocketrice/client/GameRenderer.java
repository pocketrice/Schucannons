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
import com.badlogic.gdx.math.Vector3;
import io.github.pocketrice.client.ui.HUD;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;

// Turns backend logic into glorious rendering. As in, take crap from GameManager that's from the server and move stuff around. All rendering is ONLY in this class.
// This should only be used for GameScreen.
public class GameRenderer {
    ModelInstance envMi, projMi;
    ModelBatch batch;
    Camera gameCam;
    CameraInputController gameCic;
    HUD hud;
    Environment env;
    GameManager gmgr;

    public static final Model ENV_MODEL = new GLBLoader().load(Gdx.files.internal("models/terrain.glb")).scene.model;
    public static final Model PROJ_MODEL = new GLTFLoader().load(Gdx.files.internal("models/cannonball.gltf")).scene.model;
    public static final Model CANNON_MODEL = new GLTFLoader().load(Gdx.files.internal("models/schucannon.gltf")).scene.model;
    public static final Vector3 CAMERA_POS = new Vector3(0f, 2f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);


    public GameRenderer(GameManager gm) {
      //  Gdx.app.postRunnable(() -> {
            batch = new ModelBatch();
            envMi = new ModelInstance(CANNON_MODEL); // TEMP - need to optimise this to load on loading screen.
            envMi.transform.scl(100f);
            projMi = new ModelInstance(PROJ_MODEL);
        //projMi = new ModelInstance(new GLTFLoader().load(Gdx.files.internal("models/avgprog.gltf")).scene.model);
            projMi.transform.scl(20f);


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
            env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 0.9f));
            env.add(new DirectionalLight().set(0.6f, 0.3f, 1.1f, 1f, -0.5f, 0f));
            env.add(new DirectionalLight().set(0.8f, 0.3f, 0.4f, 0f, 0.5f, 0f));
       // });
    }

    public void render() {
       // Gdx.app.postRunnable(new Runnable() {
//            @Override
//            public void run() {
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
                gameCam.update();
                gameCic.update();
                batch.begin(gameCam);
                batch.render(envMi, env);
                batch.render(projMi, env);
                batch.end();
               // hud.render(); // todo render on other screen
       //     }
            // System.out.println("GE RENDER");

       // });
    }

    public void update() {
        Match matchState = gmgr.getMatchState();
        System.out.println("TODO: RENDER UPDATE CRAP");
    }

    public void dispose() {
        batch.dispose();
        hud.dispose();
    }
}
