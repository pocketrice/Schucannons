package io.github.pocketrice.client.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public class SchuButton extends TextButton {

    public SchuButton(String text, Skin skin) {
        super(text, skin);

        this.addListener(new ChangeListener(){
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("changed");
            }
        });
    }


}
