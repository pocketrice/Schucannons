package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.Fontbook;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.screens.GameScreen;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import io.github.pocketrice.shared.Request;
import lombok.Getter;

public class StartButton extends SchuButton {
    GameManager gman;
    GameScreen gscreen;



    public StartButton(GameManager gm, GameScreen gs, Audiobox ab, Fontbook fb, Skin skin) {
        super("TO WAR!", skin);
        this.setHeight(100);
        audiobox = ab;
        fontbook = fb;
        tbs = new TextButtonStyle();
        tbs.font = fontbook.getSizedBitmap("tf2build", 60);
        this.setStyle(tbs);



        gman = gm;
        gscreen = gs;

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
                    gman.getClient().getSelf().setReady(true);
                    gman.getMatchState().updateState();
                    gman.getClient().getKryoClient().sendTCP(new Request("GC_ready", gman.getMatchState().getMatchId() + "|" + gman.getClient().getSelf().getPlayerId()));
                    gscreen.finishPrompt();
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
               // audiobox.playSfx("buttonrollover", 2f);
                interlerpFontSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
//                interlerpRot.setInterlerpStatus(true);
                interlerpFontSize.setForward(false);
                interlerpColor.setForward(false);
//                interlerpRot.setDirection(false);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                audiobox.playSfx("hint", 5f);
                interlerpFontSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
//                interlerpRot.setInterlerpStatus(true);
                interlerpFontSize.setForward(true);
                interlerpColor.setForward(true);
//                interlerpRot.setDirection(true);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });

    }


}
