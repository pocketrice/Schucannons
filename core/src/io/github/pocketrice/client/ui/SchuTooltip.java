package io.github.pocketrice.client.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class SchuTooltip extends Label {

    public SchuTooltip(CharSequence text, Skin skin) {
        super(text, skin);
    }

    // todo: adds a listener to the parent object, which translates this label around?
    // see kryptos for example tooltip.
}
