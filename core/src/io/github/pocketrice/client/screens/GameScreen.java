package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.GameRenderer;

public class GameScreen extends ScreenAdapter {
    private final Fontbook fontbook;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private GameRenderer grdr;
    private GameManager gmgr;

    public GameScreen(GameRenderer gr, GameManager gm) {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        grdr = gr;
        gmgr = gm;
        fontbook = Fontbook.of("koholint.ttf", "dina.ttc", "tf2build.ttf", "tf2segundo.ttf", "delfino.ttf", "kyomadoka.ttf");
        fontbook.setBatch(batch);
    }

    @Override
    public void dispose() {
        batch.dispose();
        grdr.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0f, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        grdr.render();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(grdr.getGameCic());
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }
}
