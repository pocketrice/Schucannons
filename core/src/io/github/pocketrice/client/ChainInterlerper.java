package io.github.pocketrice.client;

import org.javatuples.Pair;

import java.util.*;

public class ChainInterlerper {
    Map<Float, Set<ChainKeyframe>> sublerps;

    public ChainInterlerper() {
        sublerps = new TreeMap<>();
    }
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



    public Set<ChainKeyframe> getSublerp(float t) {
        return sublerps.get(t);
    }

    public void apply(float deltaT, double stepSize) {
         List<Float> times = sublerps.keySet().stream().toList();
         for (float time : times) {
             if (deltaT >= time) {
                 sublerps.get(time).forEach(ChainKeyframe::step);
             }
         }
    }
}
