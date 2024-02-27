package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.screens.LoadScreen;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.UUID;

import static io.github.pocketrice.client.SchuGame.fontbook;

public class SchuMenuButton extends SchuButton {
    @Getter
    Label labelMatchPeek;

    public SchuMenuButton(GameManager gmgr, String text, TextButtonStyle tbs) {
        super(text.split("\\|")[1], tbs);

        LabelStyle anonStyleMi = new LabelStyle(); // Anonymous labels are used.
        anonStyleMi.font = fontbook.getSizedBitmap("koholint", 20);
        anonStyleMi.fontColor = Color.valueOf("#e7bbe5b0");
        String[] matchInfo = text.split("\\|");
        labelMatchPeek = new Label(matchInfo[2] + " (" + matchInfo[3] + ")", anonStyleMi);

        interlerps.add(LinkInterlerper.generateFontLinkLerp(this.getStyle(), 21, 24, EasingFunction.EASE_IN_OUT_SINE));
        interlerps.add(LinkInterlerper.generateOpacityLinkLerp(new Batchable(labelMatchPeek), 0f, 1f, EasingFunction.EASE_IN_OUT_SINE));
        interlerps.add(LinkInterlerper.generateColorLinkLerp(new Batchable(this), Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE));

        interlerps.forEach(il -> il.setInterlerp(false));

        activeFunc = (activeObjs) -> {
            GameManager gm = (GameManager) activeObjs.get(0);
            gm.sendSelMatch(UUID.fromString(text.split("\\|")[0]));

            Game game = ((Game) Gdx.app.getApplicationListener());
            game.setScreen(new LoadScreen((SchuGame) game));
        };
    }
}
