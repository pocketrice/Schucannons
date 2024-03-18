package io.github.pocketrice.client;

import com.badlogic.gdx.InputAdapter;

public class SchuKeyboardInput extends InputAdapter {
    @Override
    public boolean keyDown (int keycode) {
        return false;
    }

    @Override
    public boolean keyUp (int keycode) {
        return false;
    }
}
