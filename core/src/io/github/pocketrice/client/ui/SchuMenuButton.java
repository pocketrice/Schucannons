package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import io.github.pocketrice.client.GameManager;
import io.github.pocketrice.client.SchuAssetManager;
import io.github.pocketrice.client.SchuGame;
import io.github.pocketrice.client.screens.LoadScreen;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

public class SchuMenuButton extends SchuButton {
    @Getter
    Label labelMatchPeek;

    public SchuMenuButton(String text, TextButtonStyle tbs, GameManager gm, SchuAssetManager sam) {
        super(text.split("\\|")[1], tbs, sam);
        amgr = sam;

        LabelStyle styleLabelPeek = new LabelStyle(); // Anonymous labels are used.
        styleLabelPeek.font = fontbook.getSizedBitmap("koholint", 20);
        styleLabelPeek.fontColor = Color.valueOf("#e7bbe5");
        String[] matchInfo = text.split("\\|");
        labelMatchPeek = new Label(matchInfo[2] + " (" + matchInfo[3] + ")", styleLabelPeek);

        interlerps.add(LinkInterlerper.generateFontTransition(this, 21, 24, EasingFunction.EASE_IN_OUT_SINE, 0.04));
        interlerps.add(LinkInterlerper.generateOpacityTransition(new Batchable(labelMatchPeek), 0f, 1f, EasingFunction.EASE_IN_OUT_SINE, 0.04));
        interlerps.add(LinkInterlerper.generateColorTransition(new Batchable(this), Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), EasingFunction.EASE_IN_OUT_SINE, 0.04));

        interlerps.forEach(il -> il.setInterlerp(false));

        activeObjs = List.of(gm);
        activeFunc = (activeObjs) -> {
            GameManager gmgr = (GameManager) activeObjs.get(0);
            gmgr.sendSelMatch(UUID.fromString(text.split("\\|")[0]));

            Game game = ((Game) Gdx.app.getApplicationListener());
            game.setScreen(new LoadScreen((SchuGame) game));
        };
    }
}
