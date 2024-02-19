package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.GameRenderer;
import io.github.pocketrice.client.ui.ReadyButton;
import lombok.Setter;

import java.util.List;

public class GameScreen extends ScreenAdapter {
    private final Fontbook fontbook = Fontbook.of("koholint.ttf", "dina.ttc", "tf2build.ttf", "tf2segundo.ttf", "delfino.ttf", "kyomadoka.ttf");
    private final Audiobox audiobox = Audiobox.of(List.of("buttonclick.ogg", "buttonclickrelease.ogg", "hint.ogg"), List.of());

    private SpriteBatch batch;
    private OrthographicCamera camera;
    private GameRenderer grdr;
    private GameManager gmgr;
    private Stage stage;

    private ReadyButton rb;
    @Setter
    private boolean isPromptReady; // terrible

    public GameScreen(GameRenderer gr, GameManager gm) {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();
        grdr = gr;
        gmgr = gm;
        fontbook.setBatch(batch);

        rb = new ReadyButton(gm, this, audiobox, fontbook, new Skin(Gdx.files.internal("skins/onett/skin/terra-mother-ui.json")));
        rb.setPosition(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f);
        stage = new Stage();
        stage.addActor(rb);
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

        if (isPromptReady) {
            batch.begin();
            rb.draw(batch, 1f);
            batch.end();
        }

        stage.act();
    }

    public void finishPrompt() {
        isPromptReady = false;
        Gdx.input.setInputProcessor(grdr.getInputMult());
    }

    @Override
    public void resize(int width, int height) {
        grdr.getPostBatch().getProjectionMatrix().setToOrtho2D(0,0,width, height);
        grdr.buildFBO(width, height);
        grdr.getVfxManager().resize(width, height);
    }
    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        isPromptReady = true;
    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }
}
