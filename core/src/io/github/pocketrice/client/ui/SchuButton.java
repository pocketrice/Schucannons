package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
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

import java.util.UUID;

public class SchuButton extends TextButton {
    Audiobox audiobox;
    Fontbook fontbook;
    @Getter
    TextButtonStyle tbs;
    @Getter
    Label labelMatchPeek;
    @Getter
    LinkInterlerper<Integer, ? super SchuButton> interlerpFontSize; // Java PECS acronym - consumers should ? super T!
    @Getter
    LinkInterlerper<Color, ? super SchuButton> interlerpColor;
    @Getter
    LinkInterlerper<Float, ? super Label> interlerpLabelOpacity;
    @Getter
    GameManager gman;

    public SchuButton(GameManager gm, Audiobox ab, Fontbook fb, String text, Skin skin) {
        super(text.split("\\|")[1], skin);
        this.setHeight(50);
        audiobox = ab;
        fontbook = fb;
        tbs = new TextButtonStyle();

        LabelStyle anonStyleMi = new LabelStyle(); // Anonymous labels are used.
        anonStyleMi.font = fontbook.getSizedBitmap("koholint", 20);
        anonStyleMi.fontColor = Color.valueOf("#e7bbe5b0");
        String[] matchInfo = text.split("\\|");

        labelMatchPeek = new Label(matchInfo[2] + " (" + matchInfo[3] + ")", anonStyleMi);

        interlerpFontSize = new LinkInterlerper<>(21, 24, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj; // vv The easing is ALWAYS linear here, because step() already applies an easing.
                    int fontSize = interlerpFontSize.interlerp(t, EasingFunction.LINEAR); // Rmeember that interlerp returns double b/c covers most numbertypes.
                    schub.tbs.font = fontbook.getSizedBitmap("koholint", fontSize);
                    schub.setStyle(tbs);
                });

        interlerpColor = new LinkInterlerper<>(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj;
                    schub.tbs.fontColor = interlerpColor.interlerp(t, EasingFunction.LINEAR);
                    schub.setStyle(tbs);
                });

        interlerpLabelOpacity = new LinkInterlerper<>(0f, 1f, EasingFunction.EASE_IN_OUT_SINE, 0.04)
                .linkObj(labelMatchPeek)
                .linkFunc((t, obj) -> {
                    Label label = (Label) obj;
                    Color labelColor = label.getColor();
                    label.setColor(labelColor.r, labelColor.g, labelColor.b, interlerpLabelOpacity.interlerp(t, EasingFunction.LINEAR));
                });

        interlerpFontSize.setInterlerp(false);
        interlerpColor.setInterlerp(false);
        interlerpLabelOpacity.setInterlerp(false);
        /*interlerpRot = new LinkInterlerper<>(0f, 10f) // todo: ilRot should be called after schubs are moved OUT of a table. pls thank u
                .linkObj(this)
                .linkFunc((t, obj) -> {
                    SchuButton schub = (SchuButton) obj;
                    schub.setRotation(interlerpRot.interlerp(t, EasingFunction.EASE_IN_OUT_CUBIC));
                });*/
        gman = gm;

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
                    gman.sendSelMatch(UUID.fromString(text.split("\\|")[0]));

                    Game game = ((Game) Gdx.app.getApplicationListener());
                    game.setScreen(new LoadScreen((SchuGame) game));
                }
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
               // audiobox.playSfx("buttonrollover", 2f);
                interlerpFontSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
                interlerpLabelOpacity.setInterlerp(true);
                interlerpFontSize.setForward(false);
                interlerpColor.setForward(false);
                interlerpLabelOpacity.setForward(false);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
            }

            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                audiobox.playSfx("hint", 5f);
                interlerpFontSize.setInterlerp(true);
                interlerpColor.setInterlerp(true);
                interlerpLabelOpacity.setInterlerp(true);
                interlerpFontSize.setForward(true);
                interlerpColor.setForward(true);
                interlerpLabelOpacity.setForward(true);

                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Hand);
            }
        });

    }


}
