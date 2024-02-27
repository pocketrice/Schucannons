package io.github.pocketrice.client.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.GameRenderer;
import io.github.pocketrice.client.ui.SchuButton;
import io.github.pocketrice.client.ui.StartButton;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Setter;

import static io.github.pocketrice.client.SchuGame.fontbook;

public class GameScreen extends ScreenAdapter {
    private SpriteBatch batch;
    private OrthographicCamera camera;
    private GameRenderer grdr;
    private GameManager gmgr;
    private Stage stage;
    private SchuButton btnStart;
    @Setter
    private boolean isPromptReady; // terrible

    public GameScreen(GameRenderer gr, GameManager gm) {
        batch = new SpriteBatch();
        camera = new OrthographicCamera();

        grdr = gr;
        gmgr = gm;
        fontbook.bind(batch);

        TextButtonStyle tbsStart = new TextButtonStyle();
        tbsStart.font = fontbook.getSizedBitmap("tf2build", 60);

        btnStart = new SchuButton("TO WAR!", tbsStart);
        btnStart.setPosition(Gdx.graphics.getWidth() / 2f - 30f, Gdx.graphics.getHeight() / 2f - 90f);
        stage = new Stage();
        stage.addActor(btnStart);
        interlerpFontSize = new LinkInterlerper<>(45, 50, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    StartButton rb = (StartButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    int fontSize = interlerpFontSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    rb.tbs.font = fontbook.getSizedBitmap("tf2build", fontSize);
                    rb.setStyle(tbs);
                });

        interlerpColor = new LinkInterlerper<>(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    StartButton rb = (StartButton) obj;
                    rb.tbs.fontColor = interlerpColor.interlerp(t, EasingFunction.LINEAR);
                    rb.setStyle(tbs);
                });
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
            if (!grdr.isPromptBlur()) {
                grdr.setPromptBlur(true);
                fontbook.font("tf2segundo").fontColor(Color.valueOf("#a2a0ddc0")).fontSize(40);
            }

            batch.begin();
            fontbook.draw("All ready?", new Vector2(Gdx.graphics.getWidth() / 2f - 70f, Gdx.graphics.getHeight() / 2f + 70f));
            btnStart.draw(batch, 1f);
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
