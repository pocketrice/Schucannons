package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.ui.BatchGroup;
import io.github.pocketrice.client.ui.SchuButton;
import io.github.pocketrice.client.ui.SchuMenuButton;
import io.github.pocketrice.shared.LinkInterlerper;

import java.util.Arrays;
import java.util.List;

import static io.github.pocketrice.shared.AnsiCode.ANSI_PURPLE;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

public class MenuScreen extends ScreenAdapter {
    Audiobox audiobox;
    Fontbook fontbook;
    SpriteBatch sprBatch;
    ModelBatch modelBatch;
    OrthographicCamera ocam;
    PerspectiveCamera pcam;
    Stage stage;
    Environment env;
    ModelInstance panoMi;
    SchuGame game;
    SchuAssetManager amgr;

    LinkInterlerper<Float, BatchGroup> interlerpTitleFade;
    LinkInterlerper<Float, BatchGroup> interlerpMenuFade;

    List<SchuMenuButton> schubs;
    Table matchlistBtns;
    boolean isUILoaded; // TEMP

    public MenuScreen(SchuGame sg) {
        game = sg;
        amgr = sg.getAmgr();
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();

        sprBatch = new SpriteBatch();
        modelBatch = new ModelBatch();
        ocam = new OrthographicCamera();
        pcam = new PerspectiveCamera(95, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        pcam.position.set(0f, -5f, 0f);
        pcam.lookAt(10f,-5f,0);
        pcam.near = 1f;
        pcam.far = 50f;
        pcam.update();

        panoMi = new ModelInstance(amgr.aliasedGet("modelPano", Model.class));
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 0.4f));
        env.add(new DirectionalLight().set(0.6f, 0.3f, 1.1f, 1f, -0.5f, 0f));
        env.add(new DirectionalLight().set(0.8f, 0.3f, 0.4f, 0f, 0.5f, 0f));

        fontbook.bind(sprBatch);

        matchlistBtns = new Table().left();
        matchlistBtns.setPosition(70, 520);
        stage = new Stage();
        stage.addActor(matchlistBtns);

        Gdx.input.setInputProcessor(stage);
        isUILoaded = false;
    }

    @Override
    public void dispose() {
        sprBatch.dispose();
        modelBatch.dispose();
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0f, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        panoMi.transform.rotate(new Quaternion(Vector3.Y, -0.12f));
        modelBatch.begin(pcam);
        modelBatch.render(panoMi, env);
        modelBatch.end();
        sprBatch.begin();
        fontbook.draw("tinyislanders", 40, "Select a match...", new Vector2(50,700));
        if (game.getGmgr().getMatchlist() == null) { // todo: only request once
            game.getGmgr().requestMatchlist();
        } else {
            if (!isUILoaded) {
                schubs = Arrays.stream(game.getGmgr().getMatchlist())
                        .map(m -> new SchuMenuButton( m, SchuButton.generateStyle("koholint", Color.valueOf("#afafdd"), 21), game.getGmgr(), amgr))
                        .toList();

                schubs.forEach(schub -> {
                    matchlistBtns.add(schub).align(Align.left).padBottom(8f).padRight(20f);
                    matchlistBtns.add(schub.getLabelMatchPeek()).align(Align.left).padBottom(8f);
                    matchlistBtns.row();
                    Color labelCol = schub.getLabelMatchPeek().getColor();
                    schub.getLabelMatchPeek().setColor(labelCol.r, labelCol.g, labelCol.b, 0f); // Set relative opacity to 0.
                });

                System.out.println(ANSI_PURPLE + "btns loaded stupid >:(" + ANSI_RESET);
                isUILoaded = true;
            }
        }
        if (isUILoaded) {
            schubs.forEach(SchuButton::step);
        }

        matchlistBtns.draw(sprBatch, 1f);
        if (schubs != null && schubs.isEmpty()) fontbook.draw("koholint", 21, "No matches found.", new Vector2(200, 700));
        sprBatch.end();
        stage.act();
    }

    @Override
    public void show() {

    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }
}
