package io.github.pocketrice.client.ui;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.shared.LinkInterlerper;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BatchGroup extends ArrayList<Batchable> {
    List<Boolean> areEnabled;


    public BatchGroup() {
        this(new Batchable[]{});
    }

    public BatchGroup(Batchable... ba) {
        this.addAll(List.of(ba));
        areEnabled = new ArrayList<>();

        this.forEach(b -> areEnabled.add(false));
    }

    public void draw(SpriteBatch batch) {
        step();

        for (int i = 0; i < areEnabled.size(); i++) {
            if (areEnabled.get(i)) {
                try {
                    this.get(i).draw(batch);
                } catch (BatchableException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void step() {
        for (int i = 0; i < areEnabled.size(); i++) {
            if (areEnabled.get(i)) {
                this.get(i).interlerps.forEach(LinkInterlerper::step);
            }
        }
    }

    @Override
    public boolean remove(Object obj) {
        int prevSize = this.size();
        this.stream().filter(b -> b.equals(obj)).forEach(b -> {
            areEnabled.remove(this.indexOf(b));
            this.remove(b);
        });
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
    public boolean add(Batchable ba) { // Not preferred b/c you can't pass in a regular Object.
        return add(ba, true);
    }

    public boolean add(Object obj, boolean isEnabled) { // superclass add() requires Batchable, which is not great since goal of helper class is to allow for adding both the wrapper and the obj.
        boolean isAdded = false;
        if (!this.contains(obj)) {
            areEnabled.add(isEnabled);
            isAdded = super.add((obj instanceof Batchable) ? (Batchable) obj : new Batchable(obj));
        }

        return isAdded;
    }

    @Override
    public boolean addAll(Collection<? extends Batchable> objs) { // Not preferred b/c you can't pass in a regular Object.
        return addAll(objs, true);
    }

    public boolean addAll(Collection<?> objs, boolean areEnabled) {
        boolean areAdded = true;
        for (Object obj : objs) {
            if (areAdded && !this.add(obj, areEnabled)) areAdded = false;
        }

        return areAdded;
    }

    @Override
    public int indexOf(Object obj) {
        int index = -1;

        for (int i = 0; i < this.size(); i++) {
            if (this.get(i).equals(obj)) {
                index = i;
            }
        }

        return index;
    }

    public void enable(int i) {
        areEnabled.set(i, true);
    }

    public void enable(Object obj) {
        enable(this.indexOf(obj));
    }



    public void disable(int i) {
        areEnabled.set(i, false);
    }

    public void disable(Object obj) {
        disable(this.indexOf(obj));
    }

    public boolean isEnabled(int i) {
        return areEnabled.get(i);
    }

    public boolean isEnabled(Object obj) {
        return isEnabled(this.indexOf(obj));
    }

    // More convenient toArray(), instead of type erasure necessitating more verbosity from the caller
    @Override @NotNull
    public Batchable[] toArray() {
        return this.toArray(new Batchable[0]);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder("{");

        for (int i = 0; i < areEnabled.size(); i++) {
            res.append("[").append(this.get(i)).append("=").append(areEnabled.get(i)).append("]");
            if (i != areEnabled.size() - 1) res.append(", ");
        }

        res.append("}");

        return res.toString();
    }
}
