package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import io.github.pocketrice.client.*;
import io.github.pocketrice.client.Match.PhaseType;
import io.github.pocketrice.client.ui.Batchable.InterlerpPreset;
import io.github.pocketrice.shared.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor.ProcessorIdentifier;
import oshi.hardware.GlobalMemory;
import oshi.hardware.GraphicsCard;
import space.earlygrey.shapedrawer.ShapeDrawer;

import java.io.InvalidObjectException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.github.pocketrice.client.GameClient.fillStr;
import static io.github.pocketrice.client.Match.truncVec;
import static io.github.pocketrice.client.Match.truncate;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_HEIGHT;
import static io.github.pocketrice.client.SchuGame.VIEWPORT_WIDTH;
import static io.github.pocketrice.shared.AnsiCode.ANSI_BLUE;
import static io.github.pocketrice.shared.AnsiCode.ANSI_RESET;

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
    private ShapeDrawer sdr;
    @Getter
    private Stage stage;
    private SpriteBatch batch;
    private BatchGroup batchGroup;
    private TextureAtlas mainSheet;

    private Sprite sprFlourish;
    private Label labelTime, labelMatchInfo, labelPhi, labelX, labelY, labelZ, labelTheta, labelMag, labelMoveLim;
    private Table sidebar;
    private Sidebar sbMove;

    private ChainInterlerper phaseStartChInterlerp; // chinter = chain interlerper
    private LinkInterlerper<Float, ? super Pair<Sprite, SpriteBatch>> phaseFlourishInterlerp;
    private LinkInterlerper<Float, ? super Batchable> phaseTimeEnterInterlerp;
    private LinkInterlerper<Float, ? super Batchable> phaseTimeExitInterlerp;
    private LinkInterlerper<Vector2, ? super Batchable> sidebarPosInterlerp;
    //private LinkInterlerper<Vector2, ? super Batchable> btnFirePosInterlerp;
    private Interlerper<Float> matchInfoOpacityInterlerp;
    private Interlerper<Float> phaseTimeOpacityInterlerp;
    private Interlerper<Integer> matchInfoWaitAnimInterlerp;

    private Vector2 debugBoxStart;
    @Setter
    private HUDState hudPromptState;
    private ProcessorIdentifier cpuInfo;
    private GraphicsCard gpuInfo;
    private GlobalMemory gmem;
    private float physMemTotal, virtualMemTotal;

    public HUD(GameManager gm, GameRenderer gr) throws InvalidObjectException {
        batch = new SpriteBatch();
        batchGroup = new BatchGroup();
        gmgr = gm;
        amgr = gmgr.getAmgr();
        grdr = gr;
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();

        SystemInfo si = new SystemInfo();
        cpuInfo = si.getHardware().getProcessor().getProcessorIdentifier();
        gpuInfo = si.getHardware().getGraphicsCards().get(0);
        gmem = si.getHardware().getMemory();
        physMemTotal = truncate((float) (gmem.getTotal() / Math.pow(1024, 3)), 1);
        virtualMemTotal = truncate((float) (gmem.getVirtualMemory().getSwapTotal() / Math.pow(1024, 3)), 1);

        loadAssets();
        sdr = new ShapeDrawer(batch, mainSheet.findRegion("1px"));
        //fontbook.bind(batch);

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
        labelPhi = new Label("0°", thetaMagPrevStyle);
        labelMag = new Label("0.0 m/s", thetaMagPrevStyle);
        labelMoveLim = new Label("0m remaining", thetaMagStyle);
        labelMoveLim.setPosition(VIEWPORT_WIDTH / 2f, VIEWPORT_HEIGHT - 30f);

        //BackgroundColor sbBg = BackgroundColor.generateSolidBg(Color.valueOf("#d3d2e97f"));
        sidebar = new Table().left();
        sidebar.setPosition(1000, 400); // Set arbitrary distance outside of viewport as initial posOffset (note: ChainInterlerper should NEVER hide/show items, assuming it may be used for scenarios where terminal visible points are used)
        //sidebar.add(thetaPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelPhi).pad(3f, 5f, 3f, 5f).center().minWidth(140);
        sidebar.row().padTop(20);
        //sidebar.add(magPreview).pad(10f, 0f, 10f, 0f);
        sidebar.add(labelMag).pad(3f, 5f, 3f, 5f).center().minWidth(140);
        sidebar.row().padTop(20);
        //sidebar.add(thetaSel).pad(10f, 10f, 10f, 10f);
        sidebar.add(new NumberButton(5f, true, "**°", SchuButton.generateStyle("sm64", Color.WHITE, 25), 80f, 15f, labelPhi)).padRight(40).right();
        sidebar.add(new NumberButton(-5f, true, "**°", SchuButton.generateStyle("sm64", Color.WHITE, 25), 80f, 15f, labelPhi)).padRight(40).right();
        sidebar.row().padTop(20);
        //sidebar.add(magSel).pad(10f, 15f, 10f, 15f);
        sidebar.add(new NumberButton( 3f, true, " m/s", SchuButton.generateStyle("sm64", Color.valueOf("#DEDEEE"), 25), 90f, 0f, labelMag)).padRight(40).right();
        sidebar.add(new NumberButton(-3f, true, " m/s", SchuButton.generateStyle("sm64", Color.valueOf("#DEDEEE"), 25), 90f, 0f, labelMag)).padRight(40).right();
        sidebar.row().padTop(20);

        phaseStartChInterlerp = new ChainInterlerper(batch)
                .prePostObjs(List.of(gmgr.getPhaseInterv()))
                .postFunc((objs) -> {
                    Interval phaseInterv = (Interval) objs.get(0);
                    phaseInterv.unflag();
                });

        phaseFlourishInterlerp = new LinkInterlerper<>(0f, 1f, EasingFunction.EASE_OUT_CUBIC, 0.012)
                .linkObj(Pair.with(sprFlourish, batch))
                .linkFunc((t, obj) -> {
                    Pair<Sprite, SpriteBatch> batchPair = (Pair) obj;
                    Sprite spr = batchPair.getValue0();
                    SpriteBatch batch = batchPair.getValue1();
                    Texture sprTexture = spr.getTexture();

                    float lerpVal = phaseFlourishInterlerp.interlerp(t, EasingFunction.LINEAR);
//                    batch.draw(
//                            sprTexture,
//                            spr.getX(),
//                            spr.getY(),
//                            sprTexture.getWidth(),
//                            sprTexture.getHeight(),
//                            sprTexture.getWidth() * lerpVal,
//                            sprTexture.getHeight(),
//                            spr.getScaleX(),
//                            spr.getScaleY(),
//                            spr.getRotation(),
//                            spr.getRegionX(),
//                            spr.getRegionY(),
//                            (int) (spr.getRegionWidth() * lerpVal),
//                            spr.getRegionHeight(),
//                            false,
//                            false);

                    // Gdx.gl.glScissor(0,0, (int) (spr.getWidth() * lerpVal), (int) (spr.getHeight() * lerpVal));
                })
                .preFunc(SPRBATCH_FUNC)
                .postFunc(SPRBATCH_FUNC);


        phaseTimeEnterInterlerp = new LinkInterlerper<>(0f, 1f, EasingFunction.EASE_IN_OUT_SINE, 0.01)
                .linkObj(labelTime)
                .linkFunc((t, obj) -> {
                    Label label = (Label) obj;

                    float lerpVal = phaseTimeEnterInterlerp.interlerp(t, EasingFunction.LINEAR);
                    label.setColor(label.getColor().r, label.getColor().g, label.getColor().b, lerpVal);
                    label.setFontScale(1 + (1 - lerpVal));
                    label.setRotation(30 * (1 - lerpVal));
                })
                .preFunc((obj) -> batchGroup.enable(obj));

        phaseTimeExitInterlerp = new LinkInterlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025)
                .linkObj(Pair.with(List.of(new Batchable(sprFlourish), new Batchable(labelTime)), gmgr.getBufferInterv()))
                .linkFunc((t, objs) -> {
                    List<Batchable> list = ((Pair<List, Interval>) objs).getValue0();
                    list.forEach(b -> {
                        try {
                            b.opacity(phaseTimeExitInterlerp.interlerp(t, EasingFunction.LINEAR));
                        } catch (BatchableException e) {
                            throw new RuntimeException(e);
                        }
                    });
                })
                .postFunc((objs) -> {
                    System.out.println(ANSI_BLUE + "POST" + ANSI_RESET);
                    System.out.println(ANSI_BLUE + "POST" + ANSI_RESET);
                    System.out.println(ANSI_BLUE + "POST" + ANSI_RESET);
                    System.out.println(ANSI_BLUE + "POST" + ANSI_RESET);
                    System.out.println(ANSI_BLUE + "POST" + ANSI_RESET);
                    ((Pair<List, Interval>) objs).getValue1().unflag();
                });
        sbMove = new Sidebar(Orientation.LEFT);
        sbMove.left();
        Sidebar.templatise(sbMove, Color.valueOf("#DD9999ff"), "movX", "m (X)", "* m (X)", 20, 0, 2f);
        Sidebar.templatise(sbMove, Color.valueOf("#99DD99ff"), "movY", "m (Y)", "* m (Y)", 20, 0, 2f);
        Sidebar.templatise(sbMove, Color.valueOf("#9999DDff"), "movZ", "m (Z)", "* m (Z)", 20, 0, 2f);
        sbMove.row(new Label("", thetaMagStyle)); // WIP blank line
        Sidebar.templatise(sbMove, Color.valueOf("#bbe0e0ff"), "theta", "°", "*°", 360, 0, 10f);
        sbMove.seal();

        sidebarPosInterlerp = LinkInterlerper.generatePosTransition(new Batchable(sidebar), new Vector2(1000, sidebar.getY()), new Vector2(700, sidebar.getY()), EasingFunction.EASE_OUT_BACK, 0.0075)
                .preFunc((obj) -> batchGroup.enable(obj));

        // btnFirePosInterlerp = LinkInterlerper.generatePosTransition(new Batchable(btnFire), new Vector2(800, btnFire.getY()), new Vector2(700, btnFire.getY()), EasingFunction.EASE_OUT_BACK, 0.01);


        phaseStartChInterlerp.addSublerp(1f, new ChainKeyframe(phaseFlourishInterlerp, batchGroup));
        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe(phaseTimeEnterInterlerp, batch));
        phaseStartChInterlerp.addSublerp(2f, new ChainKeyframe(sidebarPosInterlerp, batch));
        batchGroup.add(phaseStartChInterlerp, false);

        matchInfoOpacityInterlerp = new Interlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025f);
        phaseTimeOpacityInterlerp = new Interlerper<>(1f, 0f, EasingFunction.EASE_IN_OUT_CUBIC, 0.0025f);
        matchInfoWaitAnimInterlerp = new Interlerper<>(0, 4, EasingFunction.LINEAR, 0.0025f);
        matchInfoWaitAnimInterlerp.setLooping(true);


        SchuButton btnFire = new SchuButton("FIRE IN THE HOLE!", SchuButton.generateStyle("sm64", Color.valueOf("#8eb4a9bf"), 18))
                .sfxDown("bw2_selmenu")
                .activeFunc((objs) -> {
                    gmgr.submitPhase();
                    finishPrompt();
                });

        btnFire.reattachListener();
        btnFire.bindInterlerp(1f, 1.05f, InterlerpPreset.SCALE, EasingFunction.EASE_IN_OUT_SINE, 0.04);
        btnFire.bindInterlerp(Color.valueOf("#8eb4a9bf"), Color.valueOf("#adccc5bf"), InterlerpPreset.COLOR, EasingFunction.EASE_IN_OUT_SINE, 0.04);
        sidebar.add(btnFire).left();

        batchGroup.add(new Batchable(btnFire)); // to be able to apply interlerps
        stage = new Stage();
        stage.addActor(sidebar);
        stage.addActor(sbMove);
    }

    public void render() {
        if (gmgr.getJoinInterv().isStamped()) {
            labelMatchInfo.setText("Waiting for players" + ".".repeat(matchInfoWaitAnimInterlerp.advance()));
            batchGroup.enable(labelMatchInfo);
        } else if (gmgr.getStartInterv().observe()) {
            int startDelaySec = (int) gmgr.getStartInterv().humanDelta(true);
            labelMatchInfo.setText("Players ready. Match starts in " + startDelaySec / 60 + ":" + ((startDelaySec % 60 < 10) ? "0" : "") + startDelaySec % 60);
            batchGroup.enable(labelMatchInfo);
        } else if (gmgr.getStartInterv().isStamped() && !gmgr.getStartInterv().observe()) {
            labelMatchInfo.setText("Match starting!");
            Color lmiColor = labelMatchInfo.getColor();
            labelMatchInfo.setColor(lmiColor.r, lmiColor.b, lmiColor.g, matchInfoOpacityInterlerp.advance());
            batchGroup.enable(labelMatchInfo);
            sbMove.requestState(Sidebar.RequestedLerpState.ACTIVATE);
        } else {
            batchGroup.disable(labelMatchInfo);
            sbMove.requestState(Sidebar.RequestedLerpState.DEACTIVATE);
        }

        batch.begin();
        batchGroup.draw(batch);
        sbMove.draw(batch, 1f);
        updatePrompt();
        renderPrompt();

        float disconCover = 0; // Cannot use isCoverAware due to overlap w/ debug. Notice that coverPad is still set; verbose but good to have.
        if (gmgr.getDisconInterv().isStamped()) {
            fontbook.reset().font("koholint").fontSize(16).fontColor(Color.valueOf("#dfd1d53f")).padY(30f);
            fontbook.formatDraw("Client disconnected... retrying in " + fillStr(Orientation.RIGHT, gmgr.getDisconInterv().humanDelta(true), '0', 4) + "s", Orientation.TOP_RIGHT, batch);
            disconCover += fontbook.getCover() + 20f;
        }

        if (SchuGame.globalGame().isDebug()) {
            fontbook.reset().font("koholint").fontSize(20).fontColor(Color.valueOf("#DFE6D17F")).padX(30f).padY(80f).coverPad(8f); // original debug coords = (30,800). VIEWPORT_HEIGHT - 800 for actual pad.
            Vector3 camPos = grdr.getGameCam().position;
            Vector3 camDir = grdr.getGameCam().direction;
            fontbook.toggleCoverAware(true);
            fontbook.formatDraw("loc: " + truncVec(camPos, 3), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("look: " + truncVec(camDir, 3), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("fps: " + Gdx.graphics.getFramesPerSecond(), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("tps: " + gmgr.getClient().getServerTps(), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("server: " + gmgr.getClient().getServerName(), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("client: " + gmgr.getClient().getClientName(), Orientation.TOP_LEFT, batch);
            fontbook.formatDraw("ping: " + gmgr.getClient().getPing(), Orientation.TOP_LEFT, batch);
            fontbook.toggleCoverAware(false);

            fontbook.toggleCoverAware(true);
            String[] deadThreads = gmgr.queryThreads();

            long totalMemApp = Runtime.getRuntime().totalMemory();
            long remMemApp = totalMemApp - Runtime.getRuntime().freeMemory();
            float remMemTotal = truncate((float) (gmem.getVirtualMemory().getVirtualInUse() / Math.pow(1024, 3)), 1);

            fontbook.formatDraw("Java " + Runtime.version().version().stream().map(Object::toString).collect(Collectors.joining(".")) + " " + (cpuInfo.isCpu64bit() ? 64 : 32) + "-bit", Orientation.TOP_RIGHT, batch);
            if ((double) Runtime.getRuntime().freeMemory() / Runtime.getRuntime().totalMemory() < 0.05) {
                System.err.println("<!> Memory warning: " + Runtime.getRuntime().freeMemory() / 1E6f + "mb remaining!");
                fontbook.fontColor(Color.valueOf("#B3666C7F"));
            } else
                fontbook.fontColor(Color.valueOf("#DFE6D17F"));
            fontbook.formatDraw("mem: " + (remMemApp * 100 / totalMemApp) + "% " + truncate((remMemApp) / 1E6f, 1) + "/" + truncate(totalMemApp / 1E6f, 2) + "mb", Orientation.TOP_RIGHT, batch);
            fontbook.formatDraw("cpu: " + Runtime.getRuntime().availableProcessors() + "x " + cpuInfo.getName(), Orientation.TOP_RIGHT, batch);
            fontbook.formatDraw("gpu: " + gpuInfo.getName() + " " + remMemTotal + "/" + physMemTotal + "+" + virtualMemTotal + "gb (used/ram+vram)", Orientation.TOP_RIGHT, batch);

            fontbook.reset().font("koholint").fontSize(16).fontColor(Color.valueOf("#fbacc04f")).padY(disconCover).coverPad(5f);
            for (String dt : deadThreads) {
                fontbook.formatDraw("Thread " + dt + " dead!", Orientation.TOP_RIGHT, batch);
            }
            fontbook.toggleCoverAware(false);

            int mouseX = Gdx.input.getX();
            int mouseY = VIEWPORT_HEIGHT - Gdx.input.getY();

            if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
                debugBoxStart = (debugBoxStart != null) ? null : new Vector2(mouseX, mouseY);
            }
            if (debugBoxStart != null) {
                int deltaX = (int) (mouseX - debugBoxStart.x);
                int deltaY = (int) (mouseY - debugBoxStart.y);

                fontbook.fontColor(Color.valueOf("#ea61aabf"));
                fontbook.fontSize(15);
                fontbook.formatDraw(deltaX + ", " + deltaY, new Vector2(debugBoxStart.x + 3f, debugBoxStart.y - 5f), batch);
                sdr.rectangle(debugBoxStart.x, debugBoxStart.y, mouseX - debugBoxStart.x, mouseY - debugBoxStart.y, Color.valueOf("#ea61aabf"));
            }

            if (!SchuGame.globalGame().isPaused()) {
                SchuGame.globalGame().hideCursor();
            }

            fontbook.fontColor(Color.valueOf("#82f2d5bf"));
            fontbook.fontSize(15);
            fontbook.formatDraw(mouseX + ", " + mouseY, new Vector2((mouseX > 850) ? mouseX - 65f : mouseX, (mouseY < 200) ? mouseY + 20f : mouseY - 20f), batch);
            fontbook.fontSize(20);
            fontbook.formatDraw("+", new Vector2(mouseX - 2f, mouseY - 2f), batch);
        }

        batch.end();
        sbMove.update();
        stage.act();
    }

    public void finishPrompt() { // To be called upon finishing a prompt.
        //phaseStartChInterlerp.reset();
        gmgr.getBufferInterv().stamp();
    }

    public void updatePrompt() { // Update prompt state
        Interval phaseInterv = gmgr.getPhaseInterv();
        if (phaseInterv.justStamped()) {
            hudPromptState = HUDState.ENTER;
            sidebarPosInterlerp.setForward(true);
            phaseInterv.flag();
        } else if (phaseInterv.justEnded()) {
            hudPromptState = HUDState.EXIT;
            sidebarPosInterlerp.setForward(false);
            phaseInterv.flag();
        } else if (!phaseInterv.isFlagged()) {
            hudPromptState = (phaseInterv.observe()) ? HUDState.ACTIVE : HUDState.HIDDEN;
        }
        //System.out.println(hudPromptState);
    }

    public void renderPrompt() { // To be called anytime a part of prompt is rendered.
        Match matchState = gmgr.getMatchState();
        PhaseType phase = matchState.getPhase();
        Vector3 projVec = /* (matchState.getCurrentPlayer() == null) ? Vector3.Zero : */matchState.getCurrentPlayer().getProjVector();
        System.out.println(projVec);
        fontbook.reset().font("benzin").fontSize(24).fontColor(Color.valueOf("#b8b1f22f"));
        fontbook.formatDraw(matchState.getIdentifier(), Orientation.TOP_RIGHT, batch);

        fontbook.reset().font("tinyislanders").fontSize(35).fontColor(Color.valueOf("#d0cee08F"));
        fontbook.formatDraw("X: " + truncate(projVec.x, 2), new Vector2(30, 110), batch);
        fontbook.formatDraw("Y: " + truncate(projVec.y, 2), new Vector2(30, 85), batch);
        fontbook.formatDraw("Z: " + truncate(projVec.z, 2), new Vector2(30, 60), batch);

        switch (hudPromptState) {
            case ENTER -> {
                int remainingSec = (int) gmgr.getPhaseInterv().humanDelta(true);
                labelTime.setText(remainingSec / 60 + ":" + fillStr("" + remainingSec % 60, '0', 2));

                if (!batchGroup.isEnabled(phaseStartChInterlerp)) batchGroup.enable(phaseStartChInterlerp);
                phaseStartChInterlerp.step(0.01f);

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
            }

            case EXIT -> {
                sidebarPosInterlerp.step(); // Retract sidebar if done (early, or forced).
                phaseStartChInterlerp.step(0.01f);
            }

            case ACTIVE -> {
                batchGroup.enable(phaseStartChInterlerp);
                phaseStartChInterlerp.step(0.01f);
            }

            case HIDDEN -> {
                batchGroup.disable(phaseStartChInterlerp);
            }
        }
//        if (phase.val >= 1) { // <0 = invalid, 0 = end, >0 = active
//
//        } else {
//            switch (phase) {
//                case ENDED -> {
//
//                }
//
//                case INVALID -> {
//
//                }
//            }
//        }
    }

    public float getPhi() {
        return Float.parseFloat(NumberButton.revertSuffix(labelPhi.getText().toString(), "**°"));
    }

    public float getMag() {
        return Float.parseFloat(NumberButton.revertSuffix(labelMag.getText().toString(), "** m/s"));
    }

    public float getTheta() {
        return sbMove.queryBtn("theta");
    }

    public Vector3 getMov() {
        return new Vector3(sbMove.queryBtn("movX"), sbMove.queryBtn("movY"), sbMove.queryBtn("movZ"));
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
        labelMatchInfo = new Label("Waiting for players", labelStyleMi);
        labelMatchInfo.setPosition(30, VIEWPORT_HEIGHT - 70);
        batchGroup.add(labelMatchInfo, true);
        batchGroup.add(sprFlourish, false);
        batchGroup.add(labelTime, 5, false);
    }

    public void dispose() {
        batch.dispose();
    }

    @AllArgsConstructor
    public enum HUDState {
        HIDDEN(-1),
        ACTIVE(0),
        ENTER(1),
        EXIT(2);

        final int val;
    }
}
