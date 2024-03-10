package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.BatchableException;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.*;
import java.util.function.Consumer;

public class ChainInterlerper {
    private Map<Float, Set<ChainKeyframe>> sublerps;
    private SpriteBatch batch;
    private Consumer<List<Object>> preFunc, postFunc;
    private List<Object> prePostObjs;

    @Getter
    private boolean isForward;
    private float elapsedTime;

    public ChainInterlerper(SpriteBatch b) {
        this(b, new Pair[]{});
    }
    @SafeVarargs
    public ChainInterlerper(SpriteBatch b, Pair<Float, ChainKeyframe>... keyframes) {
        isForward = true;
        sublerps = new TreeMap<>();
        for (Pair<Float, ChainKeyframe> kf : keyframes) {
            addSublerp(kf.getValue0(), kf.getValue1());
        }
    }

    public ChainInterlerper preFunc(Consumer<List<Object>> func) {
        preFunc = func;
        return this;
    }

    public ChainInterlerper postFunc(Consumer<List<Object>> func) {
        postFunc = func;
        return this;
    }

    public ChainInterlerper prePostObjs(List<Object> objs) {
        prePostObjs = objs;
        return this;
    }

    public void addSublerp(float t, ChainKeyframe ckf) {
        sublerps.putIfAbsent(t, new HashSet<>());
        sublerps.get(t).add(ckf);
    }

    public void changeSublerpTarget(ChainKeyframe ckf, Object newTarget) {
        ckf.linkInterlerp.setLinkObj(newTarget);
    }

    public Set<ChainKeyframe> getSublerps(float t) {
        return sublerps.get(t);
    }

    public <U> float getSublerpTime(ChainKeyframe<U> ckf) {
        float time = -1;

        for (float t : sublerps.keySet()) {
            if (sublerps.get(t).contains(ckf)) time = t;
        }

        return time;
    }

    public List<ChainKeyframe> getLerps() {
        List<ChainKeyframe> lerps = new ArrayList<>();
        for (Set<ChainKeyframe> keyframes : sublerps.values()) {
            lerps.addAll(keyframes);
        }

        return lerps;
    }

    public void step(float deltaStep) {
        List<Float> times = sublerps.keySet().stream().toList();

        elapsedTime = Math.min(times.get(times.size()-1), Math.max(0, (isForward) ? elapsedTime + deltaStep : elapsedTime - deltaStep));
        if (preFunc != null && checkTerminated()) preFunc.accept(prePostObjs);
        else if (postFunc != null && checkTerminated()) postFunc.accept(prePostObjs);

         for (float time : times) {
             if ((isForward) ? elapsedTime >= time : elapsedTime <= time) {
                 sublerps.get(time).forEach(ChainKeyframe::step);
                 //System.out.println("STEP " + time);
             }
         }
    }

    // Should not be called if CKFs do not all contain Batchables. Must account for fact that at times linkObj may be bundled w/ SpriteBatch.
    public void draw(SpriteBatch batch) {
        getLerps().stream()
                //.filter(l -> Batchable.isBatchable((l.linkInterlerp.getLinkObj() instanceof Pair pair) ? pair.getValue0() : l.linkInterlerp.getLinkObj()))
                .filter(l -> elapsedTime >= getSublerpTime(l) && !(l.linkInterlerp.getLinkObj() instanceof Pair)) // Assuming Pair uses a batch — if more raw batch ops are used, there's no need to draw its obj.
                .map(l -> new Batchable(l.linkInterlerp.getLinkObj()))
                .forEach(b -> {
                    try {
                        //System.out.println(b.getX() + ", " + b.getY());
                        b.draw(batch);
                    } catch (BatchableException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void setForward(boolean isForward) {
        this.isForward = isForward;
        getLerps().forEach(l -> l.linkInterlerp.setForward(isForward));
    }

    public boolean checkTerminated() {
        List<Float> times = sublerps.keySet().stream().toList();
        return sublerps.get(times.get((isForward) ? times.size()-1 : 0)).stream().noneMatch(k -> k.linkInterlerp.isInterlerp());
    }

    public boolean reset() {
        boolean isChanged = (elapsedTime != 0 || !isForward);
        elapsedTime = 0;
        isForward = true;

        return isChanged;
    }
}
