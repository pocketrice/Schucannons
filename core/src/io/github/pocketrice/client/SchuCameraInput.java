package io.github.pocketrice.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.badlogic.gdx.math.Vector3;

import static io.github.pocketrice.client.ui.NumberButton.clamp;

public class SchuCameraInput extends FirstPersonCameraController { // Adapted from FPCC
    // Mouse to move cam
    // WASD to, well, WASD
    // Shift/Space to down/up
    public int camLockKey = Input.Keys.SLASH;
    public int rotLeftKey = Input.Keys.LEFT;
    public int rotRightKey = Input.Keys.RIGHT;
    public int rotUpKey = Input.Keys.UP;
    public int rotDownKey = Input.Keys.DOWN;
    public boolean isCamLocked;

    public SchuCameraInput(Camera camera) {
        super(camera);
        super.upKey = Input.Keys.SPACE;
        super.downKey = Input.Keys.SHIFT_LEFT;
        isCamLocked = false;

        updateCursor(); // Load initial cursor state
    }

    public void updateDeltaLook() {
        float deltaX = -Gdx.input.getDeltaX() * degreesPerPixel;
        float deltaY = -Gdx.input.getDeltaY() * degreesPerPixel;
        camera.direction.rotate(camera.up, deltaX);
        tmp.set(camera.direction).crs(camera.up).nor();

        // Copied from external LibGDX gimbal lock solution
        Vector3 oldPitchAxis = tmp.set(camera.direction).crs(camera.up).nor();
        Vector3 newDirection = tmp.cpy().set(camera.direction).rotate(tmp, deltaY);
        Vector3 newPitchAxis = tmp.cpy().set(newDirection).crs(camera.up);
        if (!newPitchAxis.hasOppositeDirection(oldPitchAxis))
            camera.direction.set(newDirection);

        camera.direction.y = clamp(camera.direction.y, -1f, 1f); // At times, swapping between cursor locks teleports camera direction
    }

    public void updateCursor() {
        SchuGame game = SchuGame.globalGame();
        if (isCamLocked) {
            game.bindCursor();
            game.lockCursor();
            game.hideCursor();
        } else {
           game.unlockCursor();
           game.restoreCursor();
        }
        System.out.println("sic camlock? " + isCamLocked);
        System.out.println("game camlock? " + game.isCursorLocked + "\n");
    }

    public void camLock(boolean isLocked) {
        isCamLocked = isLocked;
        updateCursor();
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean res = super.keyDown(keycode);

        if (keys.containsKey(camLockKey)) { // Trigger once
            SchuGame.globalAmgr().getAudiobox().playSfx("hl2_buttonclickrelease", 1f);
            isCamLocked = !isCamLocked;
            updateCursor();
        }

        return res;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isCamLocked) {
            updateDeltaLook();
        }

        return !isCamLocked;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (isCamLocked) {
            updateDeltaLook();
        }

        return isCamLocked;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float candVelo = velocity + amountY;
        velocity = Math.max(5, Math.min(20, candVelo));
        SchuGame.globalAmgr().getAudiobox().playSfx((velocity == candVelo) ? "buttonrollover" : "", 1f);

//        if (!isCamLocked) {
//            camera.direction.rotate(camera.up, amountX * 10f);
//        }

        return (amountX != 0 || amountY != 0);
    }
    @Override
    public void update(float deltaTime) {
        // Do not allow vertical movement for WASD — the vector is instead flattened to 2D.
        if (keys.containsKey(forwardKey)) {
            tmp.set(camera.direction.x, 0, camera.direction.z).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(backwardKey)) {
            tmp.set(camera.direction.x, 0, camera.direction.z).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(strafeLeftKey)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(strafeRightKey)) {
            tmp.set(camera.direction).crs(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(upKey)) {
            tmp.set(camera.up).nor().scl(deltaTime * velocity);
            camera.position.add(tmp);
        }
        if (keys.containsKey(downKey)) {
            tmp.set(camera.up).nor().scl(-deltaTime * velocity);
            camera.position.add(tmp);
        }

        if (!isCamLocked && keys.containsKey(rotLeftKey)) {
            camera.rotateAround(camera.position, camera.up, -deltaTime * degreesPerPixel);
        }
        if (!isCamLocked && keys.containsKey(rotRightKey)) {
            camera.rotateAround(camera.position, camera.up, deltaTime * degreesPerPixel);
        }
        if (!isCamLocked && keys.containsKey(rotDownKey)) {
            camera.rotateAround(camera.position, camera.up.cpy().rotate(camera.up, 90), -deltaTime * degreesPerPixel);
        }
        if (!isCamLocked && keys.containsKey(rotUpKey)) {
            camera.rotateAround(camera.position, camera.up.cpy().rotate(camera.up, 90), deltaTime * degreesPerPixel);
        }

        if (autoUpdate) camera.update(true);
    }

    public void purge() {
        keys.clear();
    }
}
