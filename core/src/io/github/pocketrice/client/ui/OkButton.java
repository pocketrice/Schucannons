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
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.screens.GameScreen;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import io.github.pocketrice.shared.Request;
import lombok.Getter;

public class OkButton extends TextButton {
    Audiobox audiobox;
    Fontbook fontbook;
    @Getter
    TextButtonStyle tbs;
    @Getter
    LinkInterlerper<Integer, ? super OkButton> interlerpFontSize; // Java PECS acronym - consumers should ? super T!
    @Getter
    LinkInterlerper<Color, ? super OkButton> interlerpColor;
    GameManager gman;
    GameScreen gscreen;



    public OkButton(GameManager gm, GameScreen gs, Audiobox ab, Fontbook fb, Skin skin) {
        super("TO WAR!", skin);
        this.setHeight(100);
        audiobox = ab;
        fontbook = fb;
        tbs = new TextButtonStyle();
        tbs.font = fontbook.getSizedBitmap("tf2build", 60);
        this.setStyle(tbs);

        interlerpFontSize = new LinkInterlerper<>(45, 50, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    OkButton rb = (OkButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    int fontSize = interlerpFontSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    rb.tbs.font = fontbook.getSizedBitmap("tf2build", fontSize);
                    rb.setStyle(tbs);
                });

        interlerpColor = new LinkInterlerper<>(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    OkButton rb = (OkButton) obj;
                    rb.tbs.fontColor = interlerpColor.interlerp(t, EasingFunction.LINEAR);
                    rb.setStyle(tbs);
                });

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
