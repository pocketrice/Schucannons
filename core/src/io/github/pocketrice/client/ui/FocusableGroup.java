package io.github.pocketrice.client.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import io.github.pocketrice.client.Audiobox;
import io.github.pocketrice.client.SchuGame;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FocusableGroup extends ArrayList<Focusable> { // Similar to BatchGroup in that it doesn't draw from LibGDX group, but no rendering is done here. Simply a container!
    List<Boolean> areStable;
    boolean isEnabled;
    int focusIndex, groupIndex;

    public FocusableGroup() {
        this(new Focusable[]{});
    }

    public FocusableGroup(Focusable... fs) {
        this(0, fs);
    }

    public FocusableGroup(int gi, Focusable... fs) {
        this.addAll(List.of(fs));
        areStable = new ArrayList<>(this.size());
        this.forEach(f -> areStable.add(true));
        focusIndex = -1;
        groupIndex = gi;
        isEnabled = false;
    }

    public void enable() {
        isEnabled = true;
    }

    public void disable() {
        isEnabled = false;
    }

    public void update() { // Called every tick
        updateStability();
        int muteIndex = -1;

        if (isEnabled) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                this.get(focusIndex).handleSel();
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                muteIndex = focusIndex;
                if (focusIndex != -1) {
                    areStable.set(focusIndex, false); // Destabilise old focusable
                }

                focusIndex = wrapIndex((Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) ? focusIndex - 1 : focusIndex + 1, this.size() - 1);
                areStable.set(focusIndex, false); // Destabilise new focusable
            }
            else if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                focusIndex = -1;
            }
        }

        for (int i = 0; i < this.size(); i++) {
            if (!areStable.get(i)) {
                if (i == muteIndex) {
                    Audiobox ab = SchuGame.globalAmgr().getAudiobox();
                    ab.mute(); // Mute exit sound
                    this.get(i).handleFocus((i == focusIndex));
                    ab.unmute();
                } else {
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
