package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static io.github.pocketrice.client.SchuGame.audiobox;
import static io.github.pocketrice.client.SchuGame.fontbook;
import static io.github.pocketrice.client.ui.HUD.DEFAULT_SKIN;

// All-purpose UI button
public class SchuButton extends TextButton {
    @Getter
    Set<LinkInterlerper> interlerps;
    Consumer<List<Object>> activeFunc;
    List<Object> activeObjs;

    public SchuButton(String text, TextButtonStyle tbs) {
        super(text.split("\\|")[1], DEFAULT_SKIN);
        this.setHeight(50);
        this.setStyle(tbs);

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

    public static TextButtonStyle generateStyle(String font, Color color, int fs) {
        TextButtonStyle tbs = new TextButtonStyle();
        tbs.font = fontbook.getSizedBitmap(font, fs);
        tbs.fontColor = color;

        return tbs;
    }
}
