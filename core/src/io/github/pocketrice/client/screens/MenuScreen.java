package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.files.FileHandle;
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
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.Viewport;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.ui.SchuButton;

import java.util.Arrays;
import java.util.List;

public class MenuScreen extends ScreenAdapter {
    SpriteBatch sprBatch;
    ModelBatch modelBatch;
    OrthographicCamera ocam;
    PerspectiveCamera pcam;
    Stage stage;
    Environment env;
    ModelInstance panoMi;
    Fontbook fontbook;
    Audiobox audiobox;
    SchuGame game;


    public static final Model PANO_MODEL = loadModel(Gdx.files.internal("models/schupano.obj"));
    public MenuScreen(SchuGame sg, Viewport vp) {
        sprBatch = new SpriteBatch();
        modelBatch = new ModelBatch();
        ocam = new OrthographicCamera();
        pcam = new PerspectiveCamera(95, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        pcam.position.set(0f, -5f, 0f);
        pcam.lookAt(10f,-5f,0);
        pcam.near = 1f;
        pcam.far = 50f;
        pcam.update();

        panoMi = new ModelInstance(PANO_MODEL);
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.5f, 0.5f, 0.7f, 0.4f));
        env.add(new DirectionalLight().set(0.6f, 0.3f, 1.1f, 1f, -0.5f, 0f));
        env.add(new DirectionalLight().set(0.8f, 0.3f, 0.4f, 0f, 0.5f, 0f));

        fontbook = Fontbook.of("tf2segundo.ttf", "tinyislanders.ttf");
        audiobox = Audiobox.of(List.of("buttonclick.ogg", "buttonclickrelease.ogg"), List.of());
        game = sg;
        stage = new Stage(vp);
        Gdx.input.setInputProcessor(stage);
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
        fontbook.draw("tinyislanders", 40, sprBatch, "Select a match...", new Vector2(20,700), 300f);
        if (game.getGmgr().getMatchlist() == null) { // todo: only request once
            game.getGmgr().requestMatchlist();
        } else {
            List<SchuButton> schubs = Arrays.stream(game.getGmgr().getMatchlist()).map(m -> new SchuButton(m, new Skin(Gdx.files.internal("skins/onett/skin/terra-mother-ui.json")))).toList();
            Table table = new Table();
            schubs.forEach(schub -> {
                schub.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        audiobox.playSfx("buttonclick", 1f);
                    }

                    @Override
                    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                        audiobox.playSfx("slide_down", 1f);
                    }

                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                        audiobox.playSfx("slide_up", 1f);
                    }
                });
                table.add(schub).align(Align.left).padBottom(8f);
                table.row();
            });
            stage.addActor(table);
            table.setPosition(200, 500);
            table.draw(sprBatch, 1f);
            //fontbook.draw("tinyislanders", sprBatch, Arrays.toString(game.getGmgr().getMatchlist()), new Vector2(10, 600));
        }
        sprBatch.end();
    }

    @Override
    public void show() {

    }

    @Override
    public void hide(){
        Gdx.input.setInputProcessor(null);
    }

    public static Model loadModel(FileHandle filehandle) {
        ModelLoader loader = new ObjLoader();
        return loader.loadModel(filehandle);
    }
}
