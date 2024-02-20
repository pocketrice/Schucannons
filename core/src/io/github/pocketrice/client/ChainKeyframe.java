package io.github.pocketrice.client;

import io.github.pocketrice.shared.LinkInterlerper;
import org.javatuples.Pair;

import java.util.function.Consumer;

public class ChainKeyframe<U> {
    LinkInterlerper<?,U> linkInterlerp;
    Consumer<U> persistFunc;

    public ChainKeyframe(LinkInterlerper<?,U> li) {
        this(li, (null));
    }
    public ChainKeyframe(LinkInterlerper<?,U> li, Consumer<U> pf) {
        linkInterlerp = li;
        persistFunc = pf;
    }

    public void step() {
        if (linkInterlerp.isInterlerp())
            linkInterlerp.step();
        else
            persistFunc.accept(linkInterlerp.getLinkObj());
    }

    public static <T,U> Pair<T,U> extractPair(Object obj) {
        return (Pair<T,U>) obj;
    }
}
