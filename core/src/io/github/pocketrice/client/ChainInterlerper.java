package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.BatchableException;
import lombok.Getter;
import org.javatuples.Pair;

import java.util.*;

public class ChainInterlerper {
    Map<Float, Set<ChainKeyframe>> sublerps;
    @Getter
    boolean isForward;

    public ChainInterlerper() {
        sublerps = new TreeMap<>();
    }
    @SafeVarargs
    public ChainInterlerper(Pair<Float, ChainKeyframe>... keyframes) {
        sublerps = new TreeMap<>();
        for (Pair<Float, ChainKeyframe> kf : keyframes) {
            addSublerp(kf.getValue0(), kf.getValue1());
        }
    }

    public void addSublerp(float t, ChainKeyframe ckf) {
        sublerps.putIfAbsent(t, new HashSet<>());
        sublerps.get(t).add(ckf);
    }

    public void changeSublerpTarget(ChainKeyframe ckf, Object newTarget) {
        ckf.linkInterlerp.setLinkObj(newTarget);
    }

    public Set<ChainKeyframe> getSublerp(float t) {
        return sublerps.get(t);
    }

    public List<ChainKeyframe> getLerps() {
        List<ChainKeyframe> lerps = new ArrayList<>();
        for (Set<ChainKeyframe> keyframes : sublerps.values()) {
            lerps.addAll(keyframes);
        }

        return lerps;
    }

    public void step(float deltaT, double stepSize) {
         List<Float> times = sublerps.keySet().stream().toList();
         for (float time : times) {
             if (deltaT >= time) {
                 sublerps.get(time).forEach(ChainKeyframe::step);
             }
         }
    }

    // Should not be called if CKFs do not all contain Batchables
    public void draw(SpriteBatch batch) {
        getLerps().stream()
                .filter(l -> Batchable.isBatchable(l.linkInterlerp.getLinkObj()))
                .map(Batchable::new)
                .forEach(b -> {
                    try {
                        b.draw(batch);
                    } catch (BatchableException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public void setForward(boolean isForward) {
        getLerps().forEach(l -> l.linkInterlerp.setForward(isForward));
    }
}
