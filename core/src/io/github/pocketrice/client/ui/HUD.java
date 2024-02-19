package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Interlerper;
import io.github.pocketrice.shared.LinkInterlerper;
import org.javatuples.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import static io.github.pocketrice.client.GameManager.MATCH_START_MAX_DELAY_SEC;
import static io.github.pocketrice.client.Match.truncate;

// The HUD should contain dynamic things (options, popups, etc) but also persistent metrics. This is to be able to dynamically move things around.
public class HUD {
    static final Consumer<Object> SPRBATCH_FUNC = (obj) -> {
        SpriteBatch batch = ((Pair<?, SpriteBatch>) obj).getValue1();
        if (!batch.isDrawing()) batch.begin();
    };

    private final Fontbook fontbook;
    private GameManager gmgr;
    private GameRenderer grdr;
    private SpriteBatch batch;
    private TextureAtlas mainSheet;
    private Sprite sprFlourish;
    private Label labelTime, labelMatchInfo;
    private Stage stage;
    private ChainInterlerper phaseStartChil; // chil = chain interlerper
    private LinkInterlerper<Float, ? super Pair<Sprite, SpriteBatch>> phaseFlourishInterlerp;
    private LinkInterlerper<Float, ? super Pair<Label, SpriteBatch>> phaseTextInterlerp;
    private Interlerper<Float> matchInfoOpacityInterlerp;
    private Interlerper<Integer> matchInfoWaitAnimInterlerp;


    public HUD(GameManager gm, GameRenderer gr) {
        batch = new SpriteBatch();
        fontbook = Fontbook.of("tinyislanders.ttf", "koholint.ttf", "dina.ttc", "tf2build.ttf", "tf2segundo.ttf", "benzin.ttf", "99occupy.ttf");
        fontbook.setBatch(batch);
        loadAssets();

        phaseStartChil = new ChainInterlerper();

        phaseFlourishInterlerp = new LinkInterlerper<>(0f, 1f)
                .linkObj(Pair.with(sprFlourish, batch))
                .linkFunc((t, obj) -> {
                    Pair<Sprite, SpriteBatch> batchPair = (Pair) obj;
                    Sprite spr = batchPair.getValue0();

                    float lerpVal = phaseFlourishInterlerp.interlerp(t, EasingFunction.LINEAR);
                    Gdx.gl.glScissor(0,0, (int) (spr.getWidth() * lerpVal), (int) (spr.getHeight() * lerpVal));
                })
                .preFunc((obj) -> {
                    Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
                    SPRBATCH_FUNC.accept(obj);
                })
                .postFunc((obj) -> {
                    batch.end();
                    Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
                    SPRBATCH_FUNC.accept(obj);
                });


        phaseTextInterlerp = new LinkInterlerper<>(0f, 1f)
                .linkObj(Pair.with(labelTime, batch))
                .linkFunc((t, obj) -> {
                    Pair<Label, SpriteBatch> batchPair = (Pair) obj;
                    Label labelTime = batchPair.getValue0();

                    float lerpVal = phaseTextInterlerp.interlerp(t, EasingFunction.LINEAR);
                    labelTime.setColor(labelTime.getColor().r, labelTime.getColor().g, labelTime.getColor().b, lerpVal);
                    labelTime.setFontScale(1 + (1 - lerpVal));
                    labelTime.setRotation(10 * (1 - lerpVal));
                })
                .preFunc(SPRBATCH_FUNC)
                .postFunc(SPRBATCH_FUNC);


        matchInfoOpacityInterlerp = new Interlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025);

        matchInfoWaitAnimInterlerp = new Interlerper<>(0, 4, EasingFunction.LINEAR, 0.005);
        matchInfoWaitAnimInterlerp.setLooping(true);

        phaseStartChil.addSublerp(0f, phaseFlourishInterlerp);
        phaseStartChil.addSublerp(3f, phaseTextInterlerp);

        gmgr = gm;
        grdr = gr;
    }


    public void render() {
        Match matchState = gmgr.getMatchState();
        PhaseType phase = gmgr.getPhaseType();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("benzin", 20, Color.valueOf("#b8b1f22F"), matchState.getIdentifier(), new Vector2(700, 800));

        fontbook.font("tinyislanders").fontSize(30).fontColor(Color.valueOf("#d0cee08F"));
        fontbook.formatDraw("X: " + projVec.x, new Vector2(30, 110));
        fontbook.formatDraw("Y: " + projVec.y, new Vector2(30, 85));
        fontbook.formatDraw("Z: " + projVec.z, new Vector2(30, 60));

        if (gmgr.getGame().isDebug()) {
            fontbook.font("koholint").fontSize(20).fontColor(Color.valueOf("#DFE6D17F"));
            Vector3 camPos = grdr.getGameCam().position;
            fontbook.formatDraw("loc: (" + truncate(camPos.x,3) + ", " + truncate(camPos.y,3) + ", " + truncate(camPos.z,3) + ")", new Vector2(30, 800));
            fontbook.formatDraw("fps: " + Gdx.graphics.getFramesPerSecond(), new Vector2(30, 780));
            fontbook.formatDraw("tps: " + gmgr.getClient().getServerTps(), new Vector2(30, 760));
            fontbook.formatDraw("server: " + gmgr.getClient().getServerName(), new Vector2(30, 740));
            fontbook.formatDraw("client: " + gmgr.getClient().getClientName(), new Vector2(30, 720));
            fontbook.formatDraw("ping: " + gmgr.getClient().getPing(), new Vector2(30, 700));

            if ((double) Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory() < 0.05) {
                System.err.println("<!> Memory warning: " + Runtime.getRuntime().freeMemory() / 1E6f + "mb remaining!");
                fontbook.fontColor(Color.valueOf("#B3666C7F"));
            } else
                fontbook.fontColor(Color.valueOf("#DFE6D17F"));

            fontbook.formatDraw("mem: " + truncate((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1E6f, 2) + "mb / " + truncate(Runtime.getRuntime().totalMemory() / 1E6f, 2) + "mb", new Vector2(30, 680));
        }

        switch (phase) {
            case MOVE, PROMPT, SIM -> {
                float deltaSec = ChronoUnit.MILLIS.between(gmgr.getPhaseStartInstant(), Instant.now()) / 1000f;
                phaseStartChil.apply(deltaSec, 0.02);

                int remainingSec = gmgr.getPhaseTime() - (int) deltaSec;
                labelTime.setText(remainingSec / 60 + ":" + ((remainingSec % 60 < 10) ? "0" : "") + remainingSec % 60);

                sprFlourish.draw(batch);
                labelTime.draw(batch, 1f);
            }

            case ENDED -> {

            }
        }

        if (gmgr.getJoinInstant() != null) {
            labelMatchInfo.setText("Waiting for players" + ".".repeat(matchInfoWaitAnimInterlerp.advance()));
            labelMatchInfo.draw(batch, 1f);
        }
        else if (gmgr.getStartInstant() != null && ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now()) < MATCH_START_MAX_DELAY_SEC) {
            int remainingSec = MATCH_START_MAX_DELAY_SEC - (int) ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now());
            labelMatchInfo.setText("Players ready. Match starts in " + remainingSec / 60 + ":" + ((remainingSec % 60 < 10) ? "0" : "") + remainingSec % 60);
            labelMatchInfo.draw(batch, 1f);
        }
        else if (gmgr.getStartInstant() != null){
            labelMatchInfo.setText("Match starting!");
            Color lmiColor = labelMatchInfo.getColor();
            labelMatchInfo.setColor(lmiColor.r, lmiColor.b, lmiColor.g, matchInfoOpacityInterlerp.advance());
            labelMatchInfo.draw(batch, 1f);
        }

        batch.end();
    }

    public void loadAssets() {
        mainSheet = new TextureAtlas(Gdx.files.internal("textures/main.atlas"));
        sprFlourish = mainSheet.createSprite("flourish");
        sprFlourish.setRotation(15f);
        sprFlourish.setPosition(30, 740);

        LabelStyle labelStyleTime = new LabelStyle();
        labelStyleTime.font = fontbook.getSizedBitmap("99occupy", 30, Color.valueOf("#D6D7E6"));
        labelTime = new Label("-:--", labelStyleTime);
        labelTime.setPosition(30, 750);

        LabelStyle labelStyleMi = new LabelStyle();
        labelStyleMi.font = fontbook.getSizedBitmap("tinyislanders", 25, Color.valueOf("#DFE6D15F"));
        labelMatchInfo = new Label("...", labelStyleMi);
        labelMatchInfo.setPosition(30, 780);
    }

    public void dispose() {
        batch.dispose();
    }
}
