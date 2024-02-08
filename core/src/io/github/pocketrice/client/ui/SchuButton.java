package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Game;
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
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.screens.LoadScreen;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

public class SchuButton extends TextButton {
    Audiobox audiobox;
    Fontbook fontbook;
    @Getter
    TextButtonStyle tbs;
    @Getter
    LinkInterlerper<Integer, ? super SchuButton> interlerpFontSize; // Java PECS acronym - consumers should ? super T!
    @Getter
    LinkInterlerper<Color, ? super SchuButton> interlerpColor;
   /* @Getter
    LinkInterlerper<Float, ? super SchuButton> interlerpRot;*/
    GameManager gman;



    public SchuButton(GameManager gm, Audiobox ab, Fontbook fb, String text, Skin skin) {
        super(text.split("\\|")[0], skin);
        this.setHeight(50);
        audiobox = ab;
        fontbook = fb;
        tbs = new TextButtonStyle();



        interlerpFontSize = new LinkInterlerper<>(21, 24)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    int fontSize = interlerpFontSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    schub.tbs.font = fontbook.getSizedBitmap("koholint", fontSize);
                    schub.setStyle(tbs);
                });

        interlerpColor = new LinkInterlerper<>(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"))
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj;
                    schub.tbs.fontColor = interlerpColor.interlerp(t, EasingFunction.LINEAR);
                    schub.setStyle(tbs);
                });

        /*interlerpRot = new LinkInterlerper<>(0f, 10f) // todo: ilRot should be called after schubs are moved OUT of a table. pls thank u
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj;
                    schub.setRotation(interlerpRot.interlerp(t, EasingFunction.EASE_IN_OUT_CUBIC));
                });*/
        gman = gm;

        this.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                audiobox.playSfx("buttonclick", 100f);
                gman.sendSelMatch(text.split("\\|")[0]);

                Game game = ((Game) Gdx.app.getApplicationListener());
                game.setScreen(new LoadScreen((SchuGame) game));
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
               // audiobox.playSfx("buttonrollover", 2f);
                interlerpFontSize.setInterlerpStatus(true);
                interlerpColor.setInterlerpStatus(true);
//                interlerpRot.setInterlerpStatus(true);
                interlerpFontSize.setDirection(false);
                interlerpColor.setDirection(false);
//                interlerpRot.setDirection(false);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                audiobox.playSfx("hint", 5f);
                interlerpFontSize.setInterlerpStatus(true);
                interlerpColor.setInterlerpStatus(true);
//                interlerpRot.setInterlerpStatus(true);
                interlerpFontSize.setDirection(true);
                interlerpColor.setDirection(true);
//                interlerpRot.setDirection(true);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });

    }


}
