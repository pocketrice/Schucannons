package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Interlerper;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;
import org.javatuples.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import static io.github.pocketrice.client.GameManager.MATCH_START_MAX_DELAY_SEC;
import static io.github.pocketrice.client.Match.truncVec;
import static io.github.pocketrice.client.Match.truncate;
import static io.github.pocketrice.client.SchuGame.*;

// The HUD should contain dynamic things (options, popups, etc) but also persistent metrics. This is to be able to dynamically move things around.
public class HUD {
    public static final Consumer<Object> SPRBATCH_FUNC = (obj) -> {
        SpriteBatch batch = ((Pair<?, SpriteBatch>) obj).getValue1();
        if (!batch.isDrawing()) batch.begin();
    };
    public static final Skin DEFAULT_SKIN = new Skin(Gdx.files.internal("skins/onett/skin/terra-mother-ui.json"));

    private GameManager gmgr;
    private GameRenderer grdr;
    private SpriteBatch batch;
    private TextureAtlas mainSheet;
    private Sprite sprFlourish;
    private Label labelTime, labelMatchInfo, labelTheta, labelMag, labelMoveLim;
    private Table sidebar;
    @Getter
    private Stage stage;
    private ChainInterlerper phaseStartChInterlerp; // chinter = chain interlerper
    private LinkInterlerper<Float, ? super Pair<Sprite, SpriteBatch>> phaseFlourishInterlerp;
    private LinkInterlerper<Float, ? super Pair<Label, SpriteBatch>> phaseTextInterlerp;
    private LinkInterlerper<Integer, ? super Pair<Table[], SpriteBatch>> thetaMagPosInterlerp;

    private Interlerper<Float> matchInfoOpacityInterlerp;
    private Interlerper<Integer> matchInfoWaitAnimInterlerp;

    public HUD(GameManager gm, GameRenderer gr) {
        batch = new SpriteBatch();
        fontbook.bind(batch);
        loadAssets();

        LabelStyle thetaMagPrevStyle = new LabelStyle();
        thetaMagPrevStyle.font = fontbook.getSizedBitmap("tf2build", 35);
        thetaMagPrevStyle.fontColor = Color.valueOf("#ffffff");

        LabelStyle thetaMagStyle = new LabelStyle();
        thetaMagStyle.font = fontbook.getSizedBitmap("tf2build", 20);
        thetaMagStyle.fontColor = Color.valueOf("#afafaf");

        LabelStyle moveLimStyle = new LabelStyle();

        Label thetaPreview = new Label("0 = ", thetaMagPrevStyle);
        Label magPreview = new Label("∥v∥ = ", thetaMagPrevStyle);
        Label thetaSel = new Label("0", thetaMagStyle);
        Label magSel = new Label("∥v∥", thetaMagStyle);
        labelTheta = new Label("0°", thetaMagPrevStyle);
        labelMag = new Label("0.0 m/s", thetaMagPrevStyle);
        labelMoveLim = new Label("0m remaining", thetaMagStyle);
        labelMoveLim.setPosition(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT - 30f);

        BackgroundColor sbBg = BackgroundColor.generateSolidBg(Color.valueOf("#d3d2e97f"));
        sidebar = new Table().left().pad(20f);
        sidebar.setBackground(sbBg);
        sidebar.setY(400);
        sidebar.add(thetaPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelTheta).left();
        sidebar.row();
        sidebar.add(magPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelMag).left();
        sidebar.row();
        sidebar.add(thetaSel).pad(10f, 10f, 10f, 10f);
        sidebar.add(new NumberButton(audiobox, fontbook, true, true, "**°", 90f, 0f, 5f, labelTheta, DEFAULT_SKIN)).pad(10f);
        sidebar.add(new NumberButton(audiobox, fontbook, false, true, "**°", 90f, 0f, 5f, labelTheta, DEFAULT_SKIN)).pad(10f);
        sidebar.row();
        sidebar.add(magSel).pad(10f, 15f, 10f, 15f);
        sidebar.add(new NumberButton(audiobox, fontbook, true, true, " m/s", 90f, 0f, 3f, labelMag, DEFAULT_SKIN)).pad(10f);
        sidebar.add(new NumberButton(audiobox, fontbook, false, true, " m/s", 90f, 0f, 3f, labelMag, DEFAULT_SKIN)).pad(10f);
        sidebar.row();
        sidebar.add(new SchuButton())

        stage = new Stage();
        stage.addActor(sidebar);

        phaseStartChInterlerp = new ChainInterlerper();

        phaseFlourishInterlerp = new LinkInterlerper<>(0f, 1f, EasingFunction.EASE_OUT_CUBIC, 0.012)
                .linkObj(Pair.with(sprFlourish, batch))
                .linkFunc((t, obj) -> {
                    Pair<Sprite, SpriteBatch> batchPair = (Pair) obj;
                    Sprite spr = batchPair.getValue0();
                    SpriteBatch batch = batchPair.getValue1();
                    Texture sprTexture = spr.getTexture();

                    float lerpVal = phaseFlourishInterlerp.interlerp(t, EasingFunction.LINEAR);
                    batch.draw(
                            sprTexture,
                            spr.getX(),
                            spr.getY(),
                            sprTexture.getWidth(),
                            sprTexture.getHeight(),
                            sprTexture.getWidth() * lerpVal,
                            sprTexture.getHeight(),
                            spr.getScaleX(),
                            spr.getScaleY(),
                            spr.getRotation(),
                            spr.getRegionX(),
                            spr.getRegionY(),
                            (int) (spr.getRegionWidth() * lerpVal),
                            spr.getRegionHeight(),
                            false,
                            false);

                  // Gdx.gl.glScissor(0,0, (int) (spr.getWidth() * lerpVal), (int) (spr.getHeight() * lerpVal));
                })
                .preFunc(SPRBATCH_FUNC)
                .postFunc(SPRBATCH_FUNC);


        phaseTextInterlerp = new LinkInterlerper<>(0f, 1f, EasingFunction.EASE_IN_OUT_SINE, 0.01)
                .linkObj(Pair.with(labelTime, batch))
                .linkFunc((t, obj) -> {
                    Pair<Label, SpriteBatch> batchPair = (Pair) obj;
                    Label labelTime = batchPair.getValue0();
                    SpriteBatch batch = batchPair.getValue1();

                    float lerpVal = phaseTextInterlerp.interlerp(t, EasingFunction.LINEAR);
                    labelTime.setColor(labelTime.getColor().r, labelTime.getColor().g, labelTime.getColor().b, lerpVal);
                    labelTime.setFontScale(1 + (1 - lerpVal));
                    labelTime.setRotation(30 * (1 - lerpVal));

                    labelTime.draw(batch, 1f);
                })
                .preFunc(SPRBATCH_FUNC)
                .postFunc(SPRBATCH_FUNC);

        thetaMagPosInterlerp = new LinkInterlerper<>(800, 700, EasingFunction.EASE_OUT_BACK, 0.01)
                .linkObj(Pair.with(sidebar, batch))
                .linkFunc((t, obj) -> {
                    Pair<Table, SpriteBatch> batchPair = (Pair) obj;
                    Table sb = batchPair.getValue0();
                    SpriteBatch batch = batchPair.getValue1();

                    int lerpVal = thetaMagPosInterlerp.interlerp(t, EasingFunction.LINEAR);
                    sb.setX(lerpVal);
                    sb.draw(batch, 1f);
                })
                .preFunc(SPRBATCH_FUNC)
                .postFunc(SPRBATCH_FUNC);


        phaseStartChInterlerp.addSublerp(1f, new ChainKeyframe(phaseFlourishInterlerp, (obj) -> {
            Pair<Sprite, SpriteBatch> batchPair = (Pair) obj;
            Sprite spr = batchPair.getValue0();
            SpriteBatch batch = batchPair.getValue1();
            Texture sprTexture = spr.getTexture();

            batch.draw(
                    sprTexture,
                    spr.getX(),
                    spr.getY(),
                    sprTexture.getWidth(),
                    sprTexture.getHeight(),
                    sprTexture.getWidth(),
                    sprTexture.getHeight(),
                    spr.getScaleX(),
                    spr.getScaleY(),
                    spr.getRotation(),
                    spr.getRegionX(),
                    spr.getRegionY(),
                    spr.getRegionWidth(),
                    spr.getRegionHeight(),
                    false,
                    false);
        }));

        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe<>(phaseTextInterlerp, (obj) -> {
            Pair<Label, SpriteBatch> pair = ChainKeyframe.extractPair(obj);
            pair.getValue0().draw(pair.getValue1(), 1f);
        }));

        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe<>(thetaMagPosInterlerp, (obj) -> {
            Pair<Table, SpriteBatch> pair = ChainKeyframe.extractPair(obj);
            pair.getValue0().draw(pair.getValue1(), 1f);
        }));

        matchInfoOpacityInterlerp = new Interlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025);
        matchInfoWaitAnimInterlerp = new Interlerper<>(0, 4, EasingFunction.LINEAR, 0.005);
        matchInfoWaitAnimInterlerp.setLooping(true);
        gmgr = gm;
        grdr = gr;
    }

    public void render() {
        Match matchState = gmgr.getMatchState();
        PhaseType phase = gmgr.getPhaseType();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("benzin", 24, Color.valueOf("#b8b1f22F"), matchState.getIdentifier(), new Vector2(700, 780));

        fontbook.font("tinyislanders").fontSize(30).fontColor(Color.valueOf("#d0cee08F"));
        fontbook.formatDraw("X: " + truncate(projVec.x, 2), new Vector2(30, 110));
        fontbook.formatDraw("Y: " + truncate(projVec.y, 2), new Vector2(30, 85));
        fontbook.formatDraw("Z: " + truncate(projVec.z, 2), new Vector2(30, 60));

        if (phase.val >= 1) { // <0 = invalid, 0 = end, >0 = active
            float deltaTime = ChronoUnit.MILLIS.between(gmgr.getPhaseStartInstant(), Instant.now()) / 1000f;
            int remainingSec = Math.max(0, gmgr.getPhaseDuration() - (int) deltaTime);

            switch (phase) {
                case MOVE -> {
                    labelMoveLim.setText(truncVec(gmgr.getClient().getSelf().getPos(), 2) + " m remaining [fix]");
                    labelMoveLim.draw(batch, 1f);
                    phaseStartChInterlerp.apply(deltaTime, 0.02);

//                if (remainingSec < 3) {
//                    phaseStartChinter.
//                }
                }
                case PROMPT, SIM -> {
                    phaseStartChInterlerp.apply(deltaTime, 0.02);
                    labelTime.setText(remainingSec / 60 + ":" + ((remainingSec % 60 < 10) ? "0" : "") + remainingSec % 60);
                }
            }
        } else {
            switch (phase) {
                case ENDED -> {

                }

                case INVALID -> {

                }
            }
        }

        if (gmgr.getJoinInstant() != null) {
            labelMatchInfo.setText("Waiting for players" + ".".repeat(matchInfoWaitAnimInterlerp.advance()));
            labelMatchInfo.draw(batch, 1f);
        }
        else if (gmgr.getStartInstant() != null && ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now()) < MATCH_START_MAX_DELAY_SEC) {
            int startDelaySec = MATCH_START_MAX_DELAY_SEC - (int) ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now());
            labelMatchInfo.setText("Players ready. Match starts in " + startDelaySec / 60 + ":" + ((startDelaySec % 60 < 10) ? "0" : "") + startDelaySec % 60);
            labelMatchInfo.draw(batch, 1f);
        }
        else if (gmgr.getStartInstant() != null){
            labelMatchInfo.setText("Match starting!");
            Color lmiColor = labelMatchInfo.getColor();
            labelMatchInfo.setColor(lmiColor.r, lmiColor.b, lmiColor.g, matchInfoOpacityInterlerp.advance());
            labelMatchInfo.draw(batch, 1f);
        }

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

        batch.end();
        stage.act();
    }

    public float getTheta() {
        return Float.parseFloat(NumberButton.revertSuffix(labelTheta.getText().toString(), "**°"));
    }

    public float getMag() {
        return Float.parseFloat(NumberButton.revertSuffix(labelMag.getText().toString(), "** m/s"));
    }

    public void loadAssets() {
        mainSheet = new TextureAtlas(Gdx.files.internal("textures/main.atlas"));
        sprFlourish = mainSheet.createSprite("flourish");
        sprFlourish.setRotation(11f);
        sprFlourish.setPosition(-770, 550); // oi mate, bloody magic nums evrywhere?? you are hereby not a programmer anymore >:((
        sprFlourish.setScale(0.3f);

        LabelStyle labelStyleTime = new LabelStyle();
        labelStyleTime.font = fontbook.getSizedBitmap("99occupy", 35, Color.valueOf("#D6D7E6D0"));
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
