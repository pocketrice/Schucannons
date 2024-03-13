package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.Flavour.FlavourType;
import io.github.pocketrice.client.ui.BatchableException;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class LoadScreen extends ScreenAdapter {
    private static final int UPDATE_FRAME_COUNT = 60;
    private static final int LOAD_DELAY_FRAME_COUNT = 300;

    private Audiobox audiobox;
    private Fontbook fontbook;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private SchuGame game;
    private SchuAssetManager amgr;
    private GameRenderer grdr;
    private GameManager gmgr;
    private Image schuLoad;

    private boolean hasStartedLoad;
    private String loadSpinner, loadInfo, loadFlavour;
    private int updateDeltaFrames, loadDelayFrames;

    public LoadScreen(SchuGame sg) {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        game = sg;
        gmgr = sg.getGmgr();

        hasStartedLoad = false;

        amgr = sg.getAmgr();
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
       // fontbook.bind(batch);

        amgr.aliasedLoad("textures/schuload-fit.png", "schuload.png", Texture.class);
        fontbook.getSizedBitmap("tf2build", 24); // Load bmf into memory
        fontbook.getSizedBitmap("koholint", 14); // Load bmf into memory
        amgr.finishLoadingAsset("schuload.png");
        loadAssets();

        schuLoad = new Image(amgr.aliasedGet("schuload.png", Texture.class));
        schuLoad.setScale(1.333f);

        loadSpinner = "Loading";
        updateDeltaFrames = 0;
        loadFlavour = Flavour.random(FlavourType.WAR_CRIES);
    }

    private void loadAssets() {
        amgr.aliasedLoad("models/env64.glb", "modelEnv64", SceneAsset.class);
        amgr.aliasedLoad("models/cannonball.gltf", "modelCannonProj", SceneAsset.class);
        amgr.aliasedLoad("models/sky_hl2.obj", "modelSky", Model.class);
        amgr.aliasedLoad("models/schucannon.glb", "modelCannonBarrel", SceneAsset.class);
        amgr.aliasedLoad("models/schuwheel.obj", "modelCannonWheel", Model.class);
        amgr.aliasedLoad("textures/main.atlas", "mainAtlas", TextureAtlas.class);
    }

    @Override
    public void dispose() {
        batch.dispose();
        game.dispose();
    }

    @Override
    public void render(float delta) {
        amgr.update();

        if (amgr.isFinished() && gmgr.isClientConnected() && loadDelayFrames > LOAD_DELAY_FRAME_COUNT) {
            grdr = new GameRenderer(gmgr);
            gmgr.setGrdr(grdr);
            game.setGrdr(grdr);

            try {
                game.setScreen(new GameScreen(game.getGmgr(), game.getGrdr()));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        } else {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            batch.begin();
            schuLoad.draw(batch, 1f);

            updateDeltaFrames++;
            if (updateDeltaFrames >= UPDATE_FRAME_COUNT) {
                loadSpinner += ".";
                updateDeltaFrames = 0;
            }
            if (loadSpinner.equals("Loading....")) {
                loadSpinner = "Loading";
            }
            loadInfo = (!amgr.isFinished()) ? "Importing " + amgr.getCurrentLoad()
                    : (!gmgr.isClientConnected()) ? "Connecting to server"
                    : loadFlavour;
            //loadMsg += ("▓".repeat((int) (10 * amgr.getProgress())) + "▒".repeat((int) (10 * (1 - amgr.getProgress()))));

            fontbook.draw("tf2build", 24, loadSpinner, new Vector2(710, 80), batch);
            fontbook.draw("koholint", 14, loadInfo, new Vector2(710, 50), batch);
            batch.end();

            loadDelayFrames++;
        }
    }

    @Override
    public void show() {

    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }
}
