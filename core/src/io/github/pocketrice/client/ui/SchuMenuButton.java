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
import io.github.pocketrice.client.ui.Batchable.InterlerpPreset;
import io.github.pocketrice.shared.EasingFunction;
import io.github.pocketrice.shared.LinkInterlerper;
import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class SchuMenuButton extends SchuButton {
    Label labelMatchPeek;

    public SchuMenuButton(String text, TextButtonStyle tbs, GameManager gm, SchuAssetManager sam) {
        super(text.split("\\|")[1], tbs, sam);
        amgr = sam;

        LabelStyle styleLabelPeek = new LabelStyle(); // Anonymous labels are used.
        styleLabelPeek.font = fontbook.getSizedBitmap("koholint", 20);
        styleLabelPeek.fontColor = Color.valueOf("#e7bbe5bf");
        String[] matchInfo = text.split("\\|");
        labelMatchPeek = new Label(matchInfo[2] + " (" + matchInfo[3] + ")", styleLabelPeek);
        Batchable baMatchPeek = new Batchable(labelMatchPeek);

        try {
            baMatchPeek.opacity(0f); // Force opacity to 0. Note that label.setColor() can't work b/c that forces absolute opacity, and Batchable relies on relative opacities.
        } catch (BatchableException e) {
            throw new RuntimeException(e);
        }

        bindInterlerp(1f, 1.075f, InterlerpPreset.SCALE, EasingFunction.EASE_IN_OUT_QUINTIC, 0.04);
        bindInterlerp(Color.valueOf("#afafdd"), Color.valueOf("#e2e5f3"), InterlerpPreset.COLOR, EasingFunction.EASE_IN_OUT_SINE, 0.04);
        bindInterlerp(LinkInterlerper.generateOpacityTransition(baMatchPeek, 0f, 1f, EasingFunction.EASE_IN_OUT_SINE, 0.04));
        interlerps.forEach(il -> il.setInterlerp(false));

        activeObjs = List.of(gm, this);
        inactiveFunc = (activeObjs) -> {
            GameManager gmgr = (GameManager) activeObjs.get(0);
            SchuMenuButton btn = (SchuMenuButton) activeObjs.get(1);
            btn.setScale(1.075f);
            gmgr.sendSelMatch(UUID.fromString(text.split("\\|")[0]));

            Game game = ((Game) Gdx.app.getApplicationListener());
            game.setScreen(new LoadScreen((SchuGame) game));
        };

        activeFunc = (activeObjs) -> {
            SchuMenuButton btn = (SchuMenuButton) activeObjs.get(1);
            btn.setScale(1.01f);
        };
    }


}
