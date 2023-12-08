package io.github.pocketrice;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.math.Vector3;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import static io.github.pocketrice.App.VIEWPORT_HEIGHT;
import static io.github.pocketrice.App.VIEWPORT_WIDTH;

public class GameEnvironment {
    ModelInstance envModel, projModel;
    ModelBatch batch;
    Camera gameCam;
    CameraInputController gameCic;
    HUD hud;
    Environment env;
    Match match;
    public static final Vector3 CAMERA_POS = new Vector3(0f, 5f, 0f), CAMERA_LOOK = new Vector3(2f, 5f, 2f);

    public GameEnvironment(Match m) {
        envModel = new ModelInstance(new GLTFLoader().load(Gdx.files.internal("models/cannonball.gltf")).scene.model);
        envModel.transform.scale(10f, 10f, 10f);
        projModel = new ModelInstance(new GLTFLoader().load(Gdx.files.internal("models/cannonball.gltf")).scene.model);
        projModel.transform.scale(2f,2f,2f);
        batch = new ModelBatch();

        match = m;
        hud = new HUD(match);
        gameCam = new PerspectiveCamera(67, VIEWPORT_WIDTH, VIEWPORT_HEIGHT);
        gameCam.position.set(CAMERA_POS);
        gameCam.lookAt(CAMERA_LOOK);
        gameCam.near = 0.1f;
        gameCam.far = 500f;
        gameCam.update();
        gameCic = new CameraInputController(gameCam);
        Gdx.input.setInputProcessor(gameCic);

        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 1f));
        env.add(new DirectionalLight().set(0.4f, 0.1f, 0.8f, 1f, -0.5f, 0f));
        env.add(new DirectionalLight().set(0.6f, 0.1f, 0.2f, 0f, 0.5f, 0f));
    }

    public void render() {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(gameCam);
        batch.render(envModel);
        batch.render(projModel);
        hud.render();
        batch.end();
    }
    public void animTurn() {
        System.out.println("anim'ed");
    }

    public void dispose() {
        batch.dispose();
        hud.dispose();
    }

    public boolean isLegal(Vector3 loc) {
        return true;
    }
}
