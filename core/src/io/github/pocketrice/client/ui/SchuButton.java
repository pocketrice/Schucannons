package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

// All-purpose UI button
public class SchuButton extends TextButton {
    SchuAssetManager amgr;
    Audiobox audiobox;
    Fontbook fontbook;

    @Getter
    Set<LinkInterlerper> interlerps;
    Consumer<List<Object>> activeFunc;
    List<Object> activeObjs;

    public SchuButton(String text, TextButtonStyle tbs, SchuAssetManager am) {
        super(text, am.get("skins/onett/skin/terra-mother-ui.json", Skin.class));

        amgr = am;
        audiobox = amgr.getAudiobox();
        fontbook = amgr.getFontbook();
        this.setHeight(50);
        this.setStyle(tbs);

        interlerps = new HashSet<>();
        for (LinkInterlerper lil : interlerps) {
            lil.setInterlerp(false);
        }

        this.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    audiobox.playSfx("buttonclick", 100f);
                }

                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (button == com.badlogic.gdx.Input.Buttons.LEFT) {
                    audiobox.playSfx("buttonclickrelease", 100f);
                    activeFunc.accept(activeObjs);
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
               // audiobox.playSfx("buttonrollover", 2f);
                for (LinkInterlerper lil : interlerps) {
                    lil.setInterlerp(true);
                    lil.setForward(false);
                }

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                audiobox.playSfx("hint", 5f);
                for (LinkInterlerper lil : interlerps) {
                    lil.setInterlerp(true);
                    lil.setForward(true);
                }

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });

    }

    public SchuButton activeFunc(Consumer<List<Object>> func) {
        activeFunc = func;
        return this;
    }

    public SchuButton activeObjs(List<Object> objs) {
        activeObjs = objs;
        return this;
    }

    public void step() {
        interlerps.forEach(LinkInterlerper::step);
    }

    public static TextButtonStyle generateStyle(String font, Color color, int fs) {
        TextButtonStyle tbs = new TextButtonStyle();
        tbs.font = Fontbook.quickFont(font, fs);
        tbs.fontColor = color;

        return tbs;
    }
}
