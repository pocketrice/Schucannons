package io.github.pocketrice.client;

import io.github.pocketrice.shared.LinkInterlerper;
import org.javatuples.Pair;

import java.util.*;

public class ChainInterlerper {
    Map<Float, Set<LinkInterlerper>> sublerps;

    public ChainInterlerper() {
        sublerps = new TreeMap<>();
    }
    public ChainInterlerper(Pair<Float, LinkInterlerper>... keyframes) {
        sublerps = new TreeMap<>();
        for (Pair<Float, LinkInterlerper> kf : keyframes) {
            addSublerp(kf.getValue0(), kf.getValue1());
        }
    }

    public void addSublerp(float t, LinkInterlerper interlerp) {
        sublerps.putIfAbsent(t, new HashSet<>());
        sublerps.get(t).add(interlerp);
    }

    public Set<LinkInterlerper> getSublerp(float t) {
        return sublerps.get(t);
    }

    public void apply(float deltaT, double stepSize) {
         List<Float> times = sublerps.keySet().stream().toList();
         for (float time : times) {
             if (deltaT >= time) {
                 sublerps.get(time).forEach(LinkInterlerper::step);
             }
         }
    }
}
