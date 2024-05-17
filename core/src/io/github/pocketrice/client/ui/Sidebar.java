package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.shared.*;

import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Map;

// Simply a table with convenience controls for transitioning and templatising
public class Sidebar extends Table {
    public static final float MARGIN_SIZE = 30f;

    private final Orientation or;
    private LinkInterlerper<Vector2, ? super Batchable> sidebarInterlerp;
    private Onceton<LinkInterlerper> otActive, otDeactive;
    private Map<String, Label> labelMap;
    private RequestedLerpState rlstate;
    private boolean isSealed;



    public Sidebar() {
        this(Orientation.RIGHT);
    }

    public Sidebar(Orientation orientation) {
        or = orientation;
        isSealed = false;
        labelMap = new HashMap<>();
        rlstate = RequestedLerpState.RESUME;
    }

    public void update() {
        switch (rlstate) {
            case ACTIVATE -> {
                otActive.execute();
                sidebarInterlerp.step();
                if (sidebarInterlerp.isTerminated()) {
                    rlstate = RequestedLerpState.RESUME;
                }
            }

            case DEACTIVATE -> {
                otDeactive.execute();
                sidebarInterlerp.step();
                if (sidebarInterlerp.isTerminated()) {
                    rlstate = RequestedLerpState.RESUME;
                }
            }

            case RESUME -> {
                otActive.reset();
                otDeactive.reset();
                sidebarInterlerp.setInterlerp(false);
            }
        }
    }

    public void requestState(RequestedLerpState rls) {
        rlstate = rls;
    }

    public void row(Actor... actors) throws InvalidObjectException {
        if (!isSealed) {
            for (Actor a : actors) {
                this.add(a).pad(3f, 5f, 3f, 5f).center().minWidth(80);
            }
            super.row().padTop(20);
        } else {
            throw new InvalidObjectException("Attempted sidebar modification but was already sealed!");
        }
    }

    public void row(int i, Actor... actors) throws InvalidObjectException {
        if (!isSealed) {
            int rowCount = this.getRows();
            if (i >= rowCount) {
                row(actors);
            } else {
                Array<Array<Actor>> allRows = extractAllRows();
                super.clear();

                for (int j = 0; j < rowCount + 1; j++) {
                    // [[ j == i? ]] - insert the row!
                    // [[ j < i? ]] - use row @ i
                    // [[ j > i? ]] - use row @ i-1
                    row((j == i) ? actors : allRows.get((j < i) ? i : i - 1).toArray());
                }
            }
        } else {
            throw new InvalidObjectException("Attempted sidebar modification but was already sealed!");
        }
    }

    // ~~ UTILITY METHODS ~~
    public Array<Actor> extractRow(int i) {
        Array<Actor> row = new Array<>();
        Array<Cell> allCells = this.getCells();

        for (int j = 0; !(allCells.get(j) == null || j < allCells.size); j++) {
            row.add(allCells.get(j).getActor());
        }

        return row;
    }

    public Array<Array<Actor>> extractAllRows() {
        Array<Array<Actor>> rows = new Array<>();
        for (int i = 0; i < this.getRows(); i++) {
            rows.add(extractRow(i));
        }

        return rows;
    }

    // ~~ END UTILITY METHODS ~~


    // Seal the items
    public void seal() {
        switch (or) {
            case LEFT -> {
                this.setPosition(-(this.getWidth() + MARGIN_SIZE * 8), SchuGame.VIEWPORT_HEIGHT / 2f);
                sidebarInterlerp = LinkInterlerper.generatePosTransition(new Batchable(this), new Vector2(this.getX(), this.getY()), new Vector2(this.getWidth() + MARGIN_SIZE, this.getY()), EasingFunction.EASE_OUT_BACK, 0.0075);
//                        .preFunc((obj) -> batchGroup.enable(obj));
            }

            case RIGHT -> {
                this.setPosition(SchuGame.VIEWPORT_WIDTH + this.getWidth() + MARGIN_SIZE, SchuGame.VIEWPORT_HEIGHT / 2f);
                sidebarInterlerp = LinkInterlerper.generatePosTransition(new Batchable(this), new Vector2(this.getX(), this.getY()), new Vector2(SchuGame.VIEWPORT_WIDTH - (this.getWidth() + MARGIN_SIZE), this.getY()), EasingFunction.EASE_OUT_BACK, 0.0075);

            }

            case TOP -> {
                this.setPosition(SchuGame.VIEWPORT_WIDTH / 2f, -(this.getHeight() + MARGIN_SIZE));
                sidebarInterlerp = LinkInterlerper.generatePosTransition(new Batchable(this), new Vector2(this.getX(), this.getY()), new Vector2(this.getX(), this.getHeight() + MARGIN_SIZE), EasingFunction.EASE_OUT_BACK, 0.0075);
            }

            case CENTER -> {
                this.setPosition(SchuGame.VIEWPORT_WIDTH / 2f, SchuGame.VIEWPORT_HEIGHT / 2f);
                sidebarInterlerp = LinkInterlerper.generatePosTransition(new Batchable(this), new Vector2(this.getX(), this.getY()), new Vector2(this.getX(), this.getY()), EasingFunction.EASE_IN_OUT_QUINTIC, 0.075);
            }
        }

        otActive = new Onceton<>((li) -> { // inactive -> active
            li.setInterlerp(true);
            li.setForward(true);
        }, sidebarInterlerp);

        otDeactive = new Onceton<>((li) -> {
            li.setInterlerp(true);
            li.setForward(false);
        }, sidebarInterlerp);

        sidebarInterlerp.setInterlerp(false);
        isSealed = true;
    }

    public void clear() {
        super.clear();
        isSealed = false;
    }

    public float queryBtn(String name) {
        Label label = labelMap.get(name);
        return Float.parseFloat(label.getText().toString().replaceAll("[^.0-9]", "")); // ÜBER BAD WORKAROUND HERE! FIXME  (some kind of auto-suffix system)
    }

    // Convenience methods for templating
    public static void templatise(Sidebar sb, Color col, String ind, String unit, String suffix, float upper, float lower, float s) throws InvalidObjectException {
        SchuAssetManager amgr = SchuGame.globalGame().getAmgr();
        TextureAtlas ms = amgr.aliasedGet("mainAtlas", TextureAtlas.class);
        Fontbook fb = SchuGame.globalAmgr().getFontbook();

        // TODO: implement "IND" (basically allow any other text content in NUMBTN. Presently not implemented yet)
        LabelStyle tpStyle = new LabelStyle();
       // tpStyle.background = new NinePatchDrawable(ms.createPatch("pokemmo"));
        tpStyle.font = fb.getSizedBitmap("tinyislanders", 35);
        tpStyle.fontColor = col.cpy();
        Label tpLabel = new Label("0 " + unit, tpStyle);

        NumberButton tpInc = new NumberButton(s, true, suffix, SchuButton.generateStyle("sm64", SchuUtils.blend(Color.valueOf("#DEDEEEFF"), col), 25), upper, lower, tpLabel);
        NumberButton tpDec = new NumberButton(-s, true, suffix, SchuButton.generateStyle("sm64", SchuUtils.blend(Color.valueOf("#DEDEEEFF"), col), 25), upper, lower, tpLabel);

        sb.labelMap.put(ind, tpLabel); // for easy access

        // Template sidebars will have only rows containing (a) label and (b) numbtns.
        System.out.println("<!> WARNING! A sidebar is being templatised. This ONLY should occur prior to non-template items (e.g. verify button). Please double-check in the event of order mishaps! :>");
        sb.row(tpLabel, tpInc, tpDec);
    }


    public enum RequestedLerpState {
        RESUME(0),
        ACTIVATE(1),
        DEACTIVATE(-1);

        RequestedLerpState(int v) {
            val = v;
        }

        final int val;
    }
}
