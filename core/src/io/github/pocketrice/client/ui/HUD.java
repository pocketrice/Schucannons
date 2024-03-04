package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.client.ui.Batchable.InterlerpPreset;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Interlerper;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;
import org.javatuples.Pair;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Consumer;

import static io.github.pocketrice.client.GameManager.START_MAX_DELAY;
import static io.github.pocketrice.client.Match.truncate;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;

// The HUD should contain dynamic things (options, popups, etc) but also persistent metrics. This is to be able to dynamically move things around.
public class HUD {
    public static final Consumer<Object> SPRBATCH_FUNC = (obj) -> {
        SpriteBatch batch = ((Pair<?, SpriteBatch>) obj).getValue1();
        if (!batch.isDrawing()) batch.begin();
    };

    private Audiobox audiobox;
    private Fontbook fontbook;
    private GameManager gmgr;
    private SchuAssetManager amgr;
    private GameRenderer grdr;
    @Getter
    private Stage stage;
    private SpriteBatch batch;
    private BatchGroup batchGroup;
    private TextureAtlas mainSheet;

    private Sprite sprFlourish;
    private Label labelTime, labelMatchInfo, labelTheta, labelMag, labelMoveLim;
    private Table sidebar;

    private ChainInterlerper phaseStartChInterlerp; // chinter = chain interlerper
    private LinkInterlerper<Float, ? super Pair<Sprite, SpriteBatch>> phaseFlourishInterlerp;
    private LinkInterlerper<Float, ? super Batchable> phaseTextInterlerp;
    private LinkInterlerper<Vector2, ? super Batchable> sidebarPosInterlerp;
    //private LinkInterlerper<Vector2, ? super Batchable> btnFirePosInterlerp;
    private Interlerper<Float> matchInfoOpacityInterlerp;
    private Interlerper<Integer> matchInfoWaitAnimInterlerp;

    public HUD(GameManager gm, GameRenderer gr) {
        gmgr = gm;
        amgr = gmgr.getAmgr();
        grdr = gr;

        batch = new SpriteBatch();
        batchGroup = new BatchGroup();

        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
        //fontbook.bind(batch);

        loadAssets();

        LabelStyle thetaMagPrevStyle = new LabelStyle();
        thetaMagPrevStyle.background = new NinePatchDrawable(mainSheet.createPatch("pokemmo"));
        thetaMagPrevStyle.font = fontbook.getSizedBitmap("tinyislanders", 35);
        thetaMagPrevStyle.fontColor = Color.valueOf("#ffffff");

        LabelStyle thetaMagStyle = new LabelStyle();
        thetaMagStyle.font = fontbook.getSizedBitmap("tinyislanders", 20);
        thetaMagStyle.fontColor = Color.valueOf("#afafaf");

        Label thetaPreview = new Label("0 = ", thetaMagPrevStyle);
        Label magPreview = new Label("∥v∥ = ", thetaMagPrevStyle);
        Label thetaSel = new Label("0", thetaMagStyle);
        Label magSel = new Label("∥v∥", thetaMagStyle);
        labelTheta = new Label("0°", thetaMagPrevStyle);
        labelMag = new Label("0.0 m/s", thetaMagPrevStyle);
        labelMoveLim = new Label("0m remaining", thetaMagStyle);
        labelMoveLim.setPosition(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT - 30f);

        //BackgroundColor sbBg = BackgroundColor.generateSolidBg(Color.valueOf("#d3d2e97f"));
        sidebar = new Table().left();
        sidebar.setPosition(800, 400);
        //sidebar.add(thetaPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelTheta).pad(3f, 5f, 3f, 5f).center().minWidth(140);
        sidebar.row().padTop(20);
        //sidebar.add(magPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelMag).pad(3f, 5f, 3f, 5f).center().minWidth(140);
        sidebar.row().padTop(20);
        //sidebar.add(thetaSel).pad(10f, 10f, 10f, 10f);
        sidebar.add(new NumberButton(true, true, "**°", SchuButton.generateStyle("sm64", Color.WHITE, 25), 90f, 0f, 5f, labelTheta, amgr)).padRight(40).right();
        sidebar.add(new NumberButton(false, true, "**°", SchuButton.generateStyle("sm64", Color.WHITE, 25), 90f, 0f, 5f, labelTheta, amgr)).padRight(40).right();
        sidebar.row().padTop(20);
        //sidebar.add(magSel).pad(10f, 15f, 10f, 15f);
        sidebar.add(new NumberButton(true, true, " m/s", SchuButton.generateStyle("sm64", Color.valueOf("#DEDEEE"), 25), 90f, 0f, 3f, labelMag, amgr)).padRight(40).right();
        sidebar.add(new NumberButton(false, true, " m/s", SchuButton.generateStyle("sm64", Color.valueOf("#DEDEEE"), 25), 90f, 0f, 3f, labelMag, amgr)).padRight(40).right();
        sidebar.row().padTop(20);
        SchuButton btnFire = new SchuButton("FIRE IN THE HOLE!", SchuButton.generateStyle("sm64", Color.valueOf("#8eb4a9bf"), 18), amgr)
                .activeFunc((objs) -> System.out.println("192.168.1.64"));
        btnFire.bindInterlerp(1f, 1.05f, InterlerpPreset.SCALE, EasingFunction.EASE_IN_OUT_SINE, 0.04);
        btnFire.bindInterlerp(Color.valueOf("#8eb4a9bf"), Color.valueOf("#adccc5bf"), InterlerpPreset.COLOR, EasingFunction.EASE_IN_OUT_SINE, 0.04);
        sidebar.add(btnFire).left();

        batchGroup.add(new Batchable(btnFire)); // to be able to apply interlerps
        stage = new Stage();
        stage.addActor(sidebar);
        //stage.addActor(btnFire);

        phaseStartChInterlerp = new ChainInterlerper(batch);

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
                .linkObj(labelTime)
                .linkFunc((t, obj) -> {
                    Label label = (Label) obj;

                    float lerpVal = phaseTextInterlerp.interlerp(t, EasingFunction.LINEAR);
                    label.setColor(label.getColor().r, label.getColor().g, label.getColor().b, lerpVal);
                    label.setFontScale(1 + (1 - lerpVal));
                    label.setRotation(30 * (1 - lerpVal));
                })
                .preFunc((obj) -> batchGroup.enable(obj));

        sidebarPosInterlerp = LinkInterlerper.generatePosTransition(new Batchable(sidebar), new Vector2(800, sidebar.getY()), new Vector2(700, sidebar.getY()), EasingFunction.EASE_OUT_BACK, 0.01)
                .preFunc((obj) -> batchGroup.enable(obj));

       // btnFirePosInterlerp = LinkInterlerper.generatePosTransition(new Batchable(btnFire), new Vector2(800, btnFire.getY()), new Vector2(700, btnFire.getY()), EasingFunction.EASE_OUT_BACK, 0.01);


        phaseStartChInterlerp.addSublerp(1f, new ChainKeyframe(phaseFlourishInterlerp));
        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe(phaseTextInterlerp, batch));
        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe(sidebarPosInterlerp, batch));
        //phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe(btnFirePosInterlerp, batch));
        batchGroup.add(phaseStartChInterlerp, false);

        matchInfoOpacityInterlerp = new Interlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025);
        matchInfoWaitAnimInterlerp = new Interlerper<>(0, 4, EasingFunction.LINEAR, 0.005);
        matchInfoWaitAnimInterlerp.setLooping(true);
    }

    public void render() {
//        if (!fontbook.getPresetBatch().equals(this.batch)) {
//            fontbook.bind(batch);
//        }
        Match matchState = gmgr.getMatchState();
        PhaseType phase = gmgr.getPhaseType();
        Vector3 projVec = (matchState.getCurrentPlayer() == null) ? Vector3.Zero : matchState.getCurrentPlayer().getProjVector();
        batch.begin();
        fontbook.formatDraw("benzin", 24, Color.valueOf("#b8b1f22F"), matchState.getIdentifier(), new Vector2(960 - 18f * matchState.getIdentifier().length(), 820), batch);

        fontbook.font("tinyislanders").fontSize(30).fontColor(Color.valueOf("#d0cee08F"));
        fontbook.formatDraw("X: " + truncate(projVec.x, 2), new Vector2(30, 110), batch);
        fontbook.formatDraw("Y: " + truncate(projVec.y, 2), new Vector2(30, 85), batch);
        fontbook.formatDraw("Z: " + truncate(projVec.z, 2), new Vector2(30, 60), batch);

        if (phase.val >= 1) { // <0 = invalid, 0 = end, >0 = active
            float deltaTime = ChronoUnit.MILLIS.between(gmgr.getPhaseStartInstant(), Instant.now()) / 1000f;
            int remainingSec = Math.max(0, gmgr.getPhaseDuration() - (int) deltaTime);

            if (!batchGroup.isEnabled(phaseStartChInterlerp)) batchGroup.enable(phaseStartChInterlerp);
            phaseStartChInterlerp.step(0.01f);
            labelTime.setText(remainingSec / 60 + ":" + ((remainingSec % 60 < 10) ? "0" : "") + remainingSec % 60);

            switch (phase) {
                case MOVE -> {
                    //labelMoveLim.setText(truncVec(gmgr.getClient().getSelf().getPos(), 2) + " m remaining [fix]");
                    //labelMoveLim.draw(batch, 1f);

//                if (remainingSec < 3) {
//                    phaseStartChinter.
//                }
                }
                case PROMPT, SIM -> {

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
            batchGroup.enable(labelMatchInfo);
        }
        else if (gmgr.getStartInstant() != null && ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now()) < START_MAX_DELAY) {
            int startDelaySec = START_MAX_DELAY - (int) ChronoUnit.SECONDS.between(gmgr.getStartInstant(), Instant.now());
            labelMatchInfo.setText("Players ready. Match starts in " + startDelaySec / 60 + ":" + ((startDelaySec % 60 < 10) ? "0" : "") + startDelaySec % 60);
            batchGroup.enable(labelMatchInfo);
        }
        else if (gmgr.getStartInstant() != null){
            labelMatchInfo.setText("Match starting!");
            Color lmiColor = labelMatchInfo.getColor();
            labelMatchInfo.setColor(lmiColor.r, lmiColor.b, lmiColor.g, matchInfoOpacityInterlerp.advance());
            batchGroup.enable(labelMatchInfo);
        }
        else {
            batchGroup.disable(labelMatchInfo);
        }

        batchGroup.draw(batch);
//        ShapeRenderer sr = new ShapeRenderer();
//        sr.setAutoShapeType(true);
//        sr.begin();
//        sidebar.drawDebug(sr);
//        sr.end();

        if (gmgr.getGame().isDebug()) {
            fontbook.font("koholint").fontSize(20).fontColor(Color.valueOf("#DFE6D17F"));
            Vector3 camPos = grdr.getGameCam().position;
            fontbook.formatDraw("loc: (" + truncate(camPos.x,3) + ", " + truncate(camPos.y,3) + ", " + truncate(camPos.z,3) + ")", new Vector2(30, 800), batch);
            fontbook.formatDraw("fps: " + Gdx.graphics.getFramesPerSecond(), new Vector2(30, 780), batch);
            fontbook.formatDraw("tps: " + gmgr.getClient().getServerTps(), new Vector2(30, 760), batch);
            fontbook.formatDraw("server: " + gmgr.getClient().getServerName(), new Vector2(30, 740), batch);
            fontbook.formatDraw("client: " + gmgr.getClient().getClientName(), new Vector2(30, 720), batch);
            fontbook.formatDraw("ping: " + gmgr.getClient().getPing(), new Vector2(30, 700), batch);

            if ((double) Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory() < 0.05) {
                System.err.println("<!> Memory warning: " + Runtime.getRuntime().freeMemory() / 1E6f + "mb remaining!");
                fontbook.fontColor(Color.valueOf("#B3666C7F"));
            } else
                fontbook.fontColor(Color.valueOf("#DFE6D17F"));

            fontbook.formatDraw("mem: " + truncate((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1E6f, 2) + "mb / " + truncate(Runtime.getRuntime().totalMemory() / 1E6f, 2) + "mb", new Vector2(30, 680), batch);

            int mouseX = Gdx.input.getX();
            int mouseY = VIEWPORT_HEIGHT - Gdx.input.getY();
            Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            fontbook.fontColor(Color.valueOf("#82f2d5bf"));
            fontbook.fontSize(15);
            fontbook.formatDraw(mouseX + ", " + mouseY, new Vector2((mouseX > 850) ? mouseX - 65f : mouseX, (mouseY < 200) ? mouseY + 20f : mouseY - 20f), batch);
            fontbook.fontSize(20);
            fontbook.formatDraw("+", new Vector2(mouseX, mouseY), batch);
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
        mainSheet = amgr.aliasedGet("mainAtlas", TextureAtlas.class);
        sprFlourish = mainSheet.createSprite("flourish");
        sprFlourish.setRotation(11f);
        sprFlourish.setPosition(-790, 550); // oi mate, bloody magic nums evrywhere?? you are hereby not a programmer anymore >:((
        sprFlourish.setScale(0.3f);

        LabelStyle labelStyleTime = new LabelStyle();
        labelStyleTime.font = fontbook.getSizedBitmap("99occupy", 35, Color.valueOf("#D6D7E6D0"));
        labelTime = new Label("-:--", labelStyleTime);
        labelTime.setPosition(30, 750);

        LabelStyle labelStyleMi = new LabelStyle();
        labelStyleMi.font = fontbook.getSizedBitmap("tinyislanders", 28, Color.valueOf("#DFE6D17F"));
        labelMatchInfo = new Label("...", labelStyleMi);
        labelMatchInfo.setPosition(30, VIEWPORT_HEIGHT - 70);
        batchGroup.add(labelMatchInfo, true);
        //System.out.println(batchGroup);
    }

    public void dispose() {
        batch.dispose();
    }
}
