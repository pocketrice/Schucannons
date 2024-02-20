package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.files.FileHandle;
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
import com.badlogic.gdx.graphics.g3d.loader.G3dModelLoader;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.JsonReader;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.ui.SchuButton;
import io.github.pocketrice.shared.LinkInterlerper;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.loaders.gltf.GLTFLoader;

import java.util.Arrays;
import java.util.List;

import static io.github.pocketrice.client.ui.HUD.DEFAULT_SKIN;
import static io.github.pocketrice.shared.AnsiCode.*;

public class MenuScreen extends ScreenAdapter {
    static final Model PANO_MODEL = loadModel(Gdx.files.internal("models/schupano.obj"));

    final Fontbook fontbook = Fontbook.of("tinyislanders.ttf", "koholint.ttf");
    final Audiobox audiobox = Audiobox.of(List.of("buttonclick.ogg", "buttonclickrelease.ogg", "buttonrollover.ogg", "hint.ogg", "notification_alert.ogg"), List.of());

    SpriteBatch sprBatch;
    ModelBatch modelBatch;
    OrthographicCamera ocam;
    PerspectiveCamera pcam;
    Stage stage;
    Environment env;
    ModelInstance panoMi;
    SchuGame game;

    LinkInterlerper<Float, Object[]> interlerpTitleFade;
    LinkInterlerper<Float, Object[]> interlerpMenuFade;

    List<SchuButton> schubs;
    Table matchlistBtns;
    boolean isUILoaded; // TEMP

    public MenuScreen(SchuGame sg) {
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

        fontbook.setBatch(sprBatch);
        game = sg;
        stage = new Stage();

        matchlistBtns = new Table().left();
        matchlistBtns.setPosition(70, 520);
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
                        .map(m -> new SchuButton(game.getGmgr(), audiobox, fontbook, m, DEFAULT_SKIN))
                        .toList();

                schubs.forEach(schub -> {
                    schub.getTbs().font = fontbook.getSizedBitmap("koholint", 21);
                    schub.getTbs().fontColor = Color.valueOf("#afafdd");
                    schub.setStyle(schub.getTbs());
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
            schubs.forEach((schub) -> {
                schub.getInterlerpColor().step();
                schub.getInterlerpFontSize().step();
                schub.getInterlerpLabelOpacity().step();
            });
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

    public static Model loadModel(FileHandle filehandle) {
        Model res;

        String[] handleStrs = filehandle.toString().split("\\.");
        switch (handleStrs[handleStrs.length - 1].toLowerCase()) {
            case "gltf" -> res = new GLTFLoader().load(filehandle).scene.model;

            case "glb" -> res = new GLBLoader().load(filehandle).scene.model;

            case "obj" -> res = new ObjLoader().loadModel(filehandle);

            case "g3db" -> res = new G3dModelLoader(new JsonReader()).loadModel(filehandle);

            default -> res = null;
        }

        return res;
    }
}
