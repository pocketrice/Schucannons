package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Cursor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FocusableGroup extends ArrayList<Focusable> { // Similar to BatchGroup in that it doesn't draw from LibGDX group, but no rendering is done here. Simply a container!
    List<Boolean> areStable;
    boolean isEnabled, isFocused, isNeedy, wasSelHeld; // Needy = needs to be updated every tick (e.g. interp is within handleFocus())
    int focusIndex, groupIndex; // <- for use if there are several FocGroups in a single UI! NOT implemented yet though. TBD. :D

    public FocusableGroup() {
        this(false);
    }

    public FocusableGroup(boolean isNeedy, Focusable... fs) {
        this(isNeedy, 0, fs);
    }

    public FocusableGroup(boolean isNeedy, int gi, Focusable... fs) {
        this.addAll(List.of(fs));
        areStable = new ArrayList<>(this.size());
        this.forEach(f -> areStable.add(true));
        focusIndex = -1;
        groupIndex = gi;
        isEnabled = false;
        isFocused = false;
        this.isNeedy = isNeedy;
        wasSelHeld = false;
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    public void update() { // Called every tick
        updateStability();
        boolean isKeyed = true;

        if (isEnabled) {
            if (isFocused) {
                Gdx.graphics.setSystemCursor(Cursor.SystemCursor.None);
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER) && isFocused) {
                this.get(focusIndex).handleSelDown();
            }
            else if (wasSelHeld && !Gdx.input.isKeyPressed(Input.Keys.ENTER)) { // Check if pressed last frame, but not pressed now (released)
                if (isFocused) {
                    this.get(focusIndex).handleSelUp();
                } else {
                    shiftFocus(true);
                }
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                shiftFocus(!Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)); // LSHIFT modifier = back, none = forward — just like browser!s
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) || isFocused && (Gdx.input.getDeltaX() != 0 || Gdx.input.getDeltaY() != 0)) {
                exitFocus();
            }
            else {
                isKeyed = false;
            }

            wasSelHeld = Gdx.input.isKeyPressed(Input.Keys.ENTER); // Save current frame isHeld for checking next frame.
        }

        if (isNeedy || isKeyed) { // If needy then always, if not needy then only if keyed.
            for (int i = 0; i < this.size(); i++) {
                if (!areStable.get(i)) {
                    this.get(i).handleFocus((i == focusIndex));
                }
            }
        }
    }

    public void updateStability() {
        for (int i = 0; i < this.size(); i++) {
            areStable.set(i, this.get(i).isStable());
        }
    }

    public void shiftFocus(boolean isForward) {
        if (focusIndex != -1) {
            areStable.set(focusIndex, false); // Destabilise old focusable
        }

        focusIndex = wrapIndex((isForward) ? focusIndex + 1 : focusIndex - 1, this.size() - 1);
        areStable.set(focusIndex, false); // Destabilise new focusable

        isFocused = true;
    }

    public void exitFocus() {
        if (focusIndex != -1) {
            areStable.set(focusIndex, false); // Destabilise old focusable
        }
        focusIndex = -1; // Stable
        Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);

        isFocused = false;
    }


    @Override
    public boolean remove(Object obj) {
        int prevSize = this.size();
        areStable.remove(this.indexOf(obj));
        super.remove(obj);
        return this.size() != prevSize;
    }

    @Override
    public boolean removeAll(Collection<?> objs) { // You would think that removeAll would simply do an interated remove() (so we could implicitly use our override), but nope! It uses an array-based batchRemove(). Sad :(
        boolean areRemoved = true;
        for (Object obj : objs) {
            if (areRemoved && !this.remove(obj)) areRemoved = false;
        }

        return areRemoved;
    }

    @Override
    public boolean add(Focusable f) { // Not preferred b/c you can't pass in a regular Object.
        boolean isAdded = false;
        if (!this.contains(f)) {
            areStable.add(true);
            isAdded = super.add(f);
        }

        return isAdded;
    }

    @Override
    public boolean addAll(Collection<? extends Focusable> fs) { // Not preferred b/c you can't pass in a regular Object.
        boolean areAdded = true;
        for (Focusable f : fs) {
            if (areAdded && !this.add(f)) areAdded = false;
        }

        return areAdded;
    }


    // More convenient toArray(), instead of type erasure necessitating more verbosity from the caller
    @NotNull
    @Override
    public Focusable[] toArray() {
        return this.toArray(new Focusable[0]);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("{");

        for (int i = 0; i < this.size(); i++) {
            res.append("[").append(this.get(i)).append("=").append(areStable.get(i)).append("]");
            if (i != this.size() - 1) res.append(", ");
        }

        res.append("}");

        return res.toString();
    }

    public static int wrapInt(int newVal, int lower, int upper) {
        return (newVal > upper) ? lower : (newVal < lower) ? upper : newVal;
    }

    public static int wrapIndex(int newIndex, int upperIndex) {
        return wrapInt(newIndex, 0, upperIndex);
    }
}
