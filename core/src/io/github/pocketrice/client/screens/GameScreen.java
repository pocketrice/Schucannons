package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuGame;

public class GameScreen extends ScreenAdapter {
    private final Fontbook fontbook;
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private SchuGame game;

    public GameScreen(SchuGame sg) {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        game = sg;
        fontbook = Fontbook.of("sm64.fnt");
    }

    @Override
    public void dispose() {
        batch.dispose();
        game.dispose();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, .25f, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        game.render();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int pointer, int button) {
                System.out.println("DOWN");
                return true;
            }
        });
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }
}
