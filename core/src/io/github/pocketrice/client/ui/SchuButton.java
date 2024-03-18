package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.client.ui.Batchable.InterlerpPreset;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.Interlerper;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// All-purpose UI button
public class SchuButton extends TextButton implements Focusable {
    SchuAssetManager amgr;
    Audiobox audiobox;
    Fontbook fontbook;

    @Getter
    Set<LinkInterlerper> interlerps;
    Consumer<List<Object>> inactiveFunc, activeFunc;
    List<Object> activeObjs;
    String sfxUp, sfxDown, sfxEnter, sfxExit;

    public SchuButton(String text, TextButtonStyle tbs, SchuAssetManager am) {
        this(text, tbs, "buttonclick", "buttonclickrelease", "hint", "", 0, 0, am);
    }

    public SchuButton minX(float x) {
        this.setWidth(Math.max(x, this.getWidth()));
        return this;
    }

    public SchuButton minY(float y) {
        this.setHeight(Math.max(y, this.getHeight()));
        return this;
    }

    public SchuButton sfxUp(String sfx) {
        sfxUp = sfx;
        return this;
    }

    public SchuButton sfxDown(String sfx) {
        sfxDown = sfx;
        return this;
    }

    public SchuButton sfxEnter(String sfx) {
        sfxEnter = sfx;
        return this;
    }

    public SchuButton sfxExit(String sfx) {
        sfxExit = sfx;
        return this;
    }

    public void reattachListener() { // Reattach listener
        this.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                audiobox.playSfx(sfxDown, 2f);
                if (activeFunc != null) activeFunc.accept(activeObjs);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                audiobox.playSfx(sfxUp, 2f);
                if (inactiveFunc != null) inactiveFunc.accept(activeObjs);
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                audiobox.playSfx(sfxExit, 2f);
                for (LinkInterlerper lil : interlerps) {
                    lil.setInterlerp(true);
                    lil.setForward(false);
                }

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                audiobox.playSfx(sfxEnter, 5f);
                for (LinkInterlerper lil : interlerps) {
                    lil.setInterlerp(true);
                    lil.setForward(true);
                }

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });
    }

    public SchuButton(String text, TextButtonStyle tbs, String sfxUp, String sfxDown, String sfxEnter, String sfxExit, float minX, float minY, SchuAssetManager am) {
        super(text, am.get("skins/onett/skin/terra-mother-ui.json", Skin.class));
        amgr = am;
        inactiveFunc = (objs) -> {};
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();

        this.setHeight(50);
        this.setStyle(tbs);

        minX(minX).minY(minY);
        sfxUp(sfxUp).sfxDown(sfxDown).sfxEnter(sfxEnter).sfxExit(sfxExit); // TODO: lombok @builder instead of this half-baked stuff

        interlerps = new HashSet<>();
        for (LinkInterlerper lil : interlerps) {
            lil.setInterlerp(false);
        }

        reattachListener();
    }

    public SchuButton inactiveFunc(Consumer<List<Object>> func) {
        inactiveFunc = func;
        return this;
    }

    public SchuButton activeFunc(Consumer<List<Object>> func) {
        activeFunc = func;
        return this;
    }

    public SchuButton activeObjs(List<Object> objs) {
        activeObjs = objs;
        return this;
    }

    public SchuButton interlerps(LinkInterlerper... lerps) {
        bindInterlerps(lerps);
        return this;
    }

    public void bindInterlerps(LinkInterlerper... lerps) {
        Arrays.stream(lerps).forEach(this::bindInterlerp);
    }

    public void bindInterlerp(LinkInterlerper lerp) {
        interlerps.add(lerp);
    }

    public <U> void bindInterlerp(U v1, U v2, InterlerpPreset preset, EasingFunction easing, double ss) {
        LinkInterlerper lil = null;
        Batchable ba = new Batchable(this);

        switch (preset) {
            case COLOR -> {
                lil = LinkInterlerper.generateColorTransition(ba, (Color) v1, (Color) v2, easing, ss);
                getStyle().downFontColor = ((Color) v2).cpy().sub(0.1f, 0.1f, 0.1f, 0f); // Update down color since end color is different now
            }
            case OPACITY -> lil = LinkInterlerper.generateOpacityTransition(ba, (float) v1, (float) v2, easing, ss);
            case POSITION -> lil = LinkInterlerper.generatePosTransition(ba, (Vector2) v1, (Vector2) v2, easing, ss);
            case ROTATION -> lil = LinkInterlerper.generateRotTransition(ba, (float) v1, (float) v2, easing, ss);
            case SCALE -> lil = LinkInterlerper.generateSclTransition(ba, (float) v1, (float) v2, easing, ss);
            //case FONT_SIZE -> lil = LinkInterlerper.generateFontTransition(this, (int) v1, (int) v2, easing, ss);
        }

        interlerps.add(lil);
    }

    public void step() {
        interlerps.forEach(LinkInterlerper::step);
    }

    @Override
    public void handleFocus(boolean isFocused) {
        InputEvent ie = new InputEvent();
        ie.setType((isFocused) ? InputEvent.Type.enter : InputEvent.Type.exit);
        this.fire(ie);
    }

    @Override
    public void handleSelDown() {
        InputEvent ie = new InputEvent();
        ie.setType(InputEvent.Type.touchDown);
        this.fire(ie);
    }

    @Override
    public void handleSelUp() {
        InputEvent ie = new InputEvent();
        ie.setType(InputEvent.Type.touchUp);
        this.fire(ie);
    }

    @Override
    public boolean isStable() {
        return interlerps.stream().noneMatch(Interlerper::isInterlerp);
    }

    public static TextButtonStyle generateStyle(String font, Color color, int fs) {
        TextButtonStyle tbs = new TextButtonStyle();
        tbs.font = Fontbook.quickFont(font, fs);
        tbs.fontColor = color;
        tbs.downFontColor = color.cpy().sub(0.05f, 0.05f, 0.05f, 0f);

        return tbs;
    }
}
