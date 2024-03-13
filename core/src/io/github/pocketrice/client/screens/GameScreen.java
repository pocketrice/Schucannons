package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.ui.BatchGroup;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.Batchable.InterlerpPreset;
import io.github.pocketrice.client.ui.BatchableException;
import io.github.pocketrice.client.ui.SchuButton;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Request;
import lombok.Setter;

import java.util.List;

public class GameScreen extends ScreenAdapter {
    private Audiobox audiobox;
    private Fontbook fontbook;
    private SpriteBatch batch;
    private BatchGroup batchGroup;
    private OrthographicCamera camera;
    private GameRenderer grdr;
    private GameManager gmgr;
    private SchuAssetManager amgr;
    private Stage stage;
    private SchuButton btnStart;

    @Setter
    private boolean isPromptReady, wasSelHeld;

    public GameScreen(GameManager gm, GameRenderer gr) throws BatchableException {
        batch = new SpriteBatch();
        batchGroup = new BatchGroup();
        camera = new OrthographicCamera();

        amgr = gm.getAmgr();
        gmgr = gm;
        grdr = gr;
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
        //fontbook.bind(batch);

        btnStart = new SchuButton("TO WAR!", SchuButton.generateStyle("tf2build", Color.valueOf("#B8B0CF"), 60), amgr)
                .activeObjs(List.of(gmgr, this))
                .activeFunc((objs) -> {
                    GameManager gman = (GameManager) objs.get(0);
                    GameScreen gsc = (GameScreen) objs.get(1);

                    gman.getClient().getSelf().setReady(true);
                    gman.getMatchState().updateState();
                    gman.getClient().getKryoClient().sendTCP(new Request("GC_ready", gman.getMatchState().getMatchId() + "|" + gman.getClient().getSelf().getPlayerId()));
                    gsc.finishPrompt();
                });

        btnStart.bindInterlerp(1f, 1.1f, InterlerpPreset.SCALE, EasingFunction.EASE_IN_OUT_QUARTIC, 0.04);
        btnStart.bindInterlerp(0f,1.5f, InterlerpPreset.ROTATION, EasingFunction.EASE_IN_OUT_CIRCULAR, 0.04);
        btnStart.bindInterlerp(Color.valueOf("#B8B0CF"), Color.valueOf("#FEFCFF"), InterlerpPreset.COLOR, EasingFunction.EASE_IN_OUT_SINE, 0.04);

        batchGroup.add(new Batchable(btnStart)
                .pos((int) (Gdx.graphics.getWidth() / 2f - 30f), (int) (Gdx.graphics.getHeight() / 2f - 60f)));
//                .bindInterlerp(45, 50, InterlerpPreset.FONT_SIZE, EasingFunction.EASE_IN_OUT_SINE, 0.04)
//                .bindInterlerp(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), InterlerpPreset.COLOR, EasingFunction.EASE_IN_OUT_SINE, 0.04), true);

        stage = new Stage();
        stage.addActor(btnStart);
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
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                InputEvent ie = new InputEvent();
                ie.setType(InputEvent.Type.touchDown);
                btnStart.fire(ie);
            }
            else if (wasSelHeld && !Gdx.input.isKeyPressed(Input.Keys.ENTER)) {
                InputEvent ie = new InputEvent();
                ie.setType(InputEvent.Type.touchUp);
                btnStart.fire(ie);
            }

            if (!grdr.isPromptBlur()) {
                grdr.setPromptBlur(true);
            }
            wasSelHeld = Gdx.input.isKeyPressed(Input.Keys.ENTER);

            batch.begin();
            fontbook.font("tf2segundo").fontColor(Color.valueOf("#a2a0ddc0")).fontSize(40);
            fontbook.draw("All ready?", new Vector2(Gdx.graphics.getWidth() / 2f - 80f, Gdx.graphics.getHeight() / 2f + 70f), batch);
            batchGroup.draw(batch);
            batch.end();
        } else {
            if (grdr.isPromptBlur()) grdr.setPromptBlur(false);
        }

        stage.act();
    }

    public void finishPrompt() {
        isPromptReady = false;
        grdr.setPromptBlur(false);
        Gdx.input.setInputProcessor(grdr.getInputMult());
    }

    @Override
    public void resize(int width, int height) {
        grdr.getPostBatch().getProjectionMatrix().setToOrtho2D(0,0,width, height);
        grdr.buildFBOs(width, height);
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
