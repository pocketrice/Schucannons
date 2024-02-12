package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.SchuGame;

public class LoadScreen extends ScreenAdapter {
    private static final int UPDATE_FRAME_COUNT = 60;
    private static final int LOAD_DELAY_FRAME_COUNT = 400;

    private final Fontbook fontbook;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private SchuGame game;
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
        schuLoad = new Image(new Texture(Gdx.files.internal("textures/schuload-fit.png")));
        schuLoad.setScale(1.333f);

        loadMsg = "Loading";
        updateDeltaFrames = 0;

        fontbook = Fontbook.of("tf2build.ttf", "tf2segundo.ttf");
        fontbook.setBatch(batch);
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
            loadMsg = "Loading";
        }
        fontbook.draw("tf2build", 24, loadMsg, new Vector2(700,70));
        batch.end();

        loadDelayFrames++;

        if (gmgr.isClientConnected() && loadDelayFrames > LOAD_DELAY_FRAME_COUNT) {
            game.setScreen(new GameScreen(game.getGrdr(), game.getGmgr()));
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
