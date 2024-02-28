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
import io.github.pocketrice.client.ui.BatchableException;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class LoadScreen extends ScreenAdapter {
    private static final int UPDATE_FRAME_COUNT = 60;
    private static final int LOAD_DELAY_FRAME_COUNT = 400;

    private Audiobox audiobox;
    private Fontbook fontbook;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private SchuGame game;
    private SchuAssetManager amgr;
    private GameRenderer grdr;
    private GameManager gmgr;
    private Image schuLoad;

    private String loadMsg;
    private int updateDeltaFrames, loadDelayFrames;

    public LoadScreen(SchuGame sg) {
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        game = sg;
        gmgr = sg.getGmgr();


        amgr = sg.getAmgr();
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
        amgr.aliasedLoad("textures/schuload-fit.png", "schuload.png", Texture.class);
        amgr.finishLoadingAsset("schuload.png");
        loadAssets(); // todo: push this to another thread (async shenanigans)

        schuLoad = new Image(amgr.aliasedGet("schuload.png", Texture.class));
        schuLoad.setScale(1.333f);

        loadMsg = "Loading";
        updateDeltaFrames = 0;

        fontbook.bind(batch);
    }

    private void loadAssets() {
        amgr.aliasedLoad("models/terrain.glb", "modelTerrain", SceneAsset.class);
        amgr.aliasedLoad("models/skypano.obj", "modelSky", Model.class);
        amgr.aliasedLoad("models/schubarrel.obj", "modelCannonBarrel", Model.class);
        amgr.aliasedLoad("models/schuwheel.obj", "modelCannonWheel", Model.class);
        amgr.aliasedLoad("models/cannonball.gltf", "modelCannonProj", SceneAsset.class);
        amgr.aliasedLoad("textures/main.atlas", "mainAtlas", TextureAtlas.class);
    }

    @Override
    public void dispose() {
        batch.dispose();
        game.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin();
        schuLoad.draw(batch, 1f);

        updateDeltaFrames++;
        if (updateDeltaFrames >= UPDATE_FRAME_COUNT) {
            loadMsg += ".";
            updateDeltaFrames = 0;
        }
        if (loadMsg.equals("Loading....")) {
            loadMsg = "Loading\n";
        }
       // loadMsg += ("▓".repeat((int) (10 * amgr.getProgress())) + "▒".repeat((int) (10 * (1 - amgr.getProgress()))));

        fontbook.draw("tf2build", 24, loadMsg, new Vector2(760,70));
        batch.end();

        loadDelayFrames++;

        if (amgr.update(16) && gmgr.isClientConnected() && loadDelayFrames > LOAD_DELAY_FRAME_COUNT) {
            grdr = new GameRenderer(gmgr);
            gmgr.setGrdr(grdr);
            game.setGrdr(grdr);

            try {
                game.setScreen(new GameScreen(game.getGmgr(), game.getGrdr()));
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
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
