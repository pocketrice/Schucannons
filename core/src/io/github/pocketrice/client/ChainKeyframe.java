package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.BatchableException;
import io.github.pocketrice.shared.LinkInterlerper;
import org.javatuples.Pair;

import java.util.function.BiConsumer;

public class ChainKeyframe<U> {
    LinkInterlerper<?, U> linkInterlerp;
    BiConsumer<U, SpriteBatch> persistFunc;
    SpriteBatch batch;

    // Assume LinkInterlerper is of type <interpObj ∋ interpObj.isBatchable, Pair<linkObj, SpriteBatch>>
    public ChainKeyframe(LinkInterlerper<?, Pair<U, SpriteBatch>> li) {
        this((LinkInterlerper<?, U>) li, li.getLinkObj().getValue1(), (pair, batch) -> {
            U targetVal = ((Pair<U, SpriteBatch>) pair).getValue0();
            Batchable targetBa = (targetVal instanceof Batchable) ? (Batchable) targetVal : new Batchable(targetVal);

            try {
                targetBa.draw(batch);
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });
    }

    // Assume LinkInterlerper is of type <interpObj ∋ interpObj.isBatchable, linkObj>
    public ChainKeyframe(LinkInterlerper<?, U> li, SpriteBatch b) {
        this(li, b, (obj, batch) -> {
            Batchable target = (obj instanceof Batchable) ? (Batchable) obj : new Batchable(obj);

            try {
                target.draw(batch);
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public ChainKeyframe(LinkInterlerper<?,U> li, SpriteBatch b, BiConsumer<U, SpriteBatch> pf) {
        linkInterlerp = li;
        linkInterlerp.setInterlerp(true);
        batch = b;
        persistFunc = pf;
    }

    public void step() {
        if (linkInterlerp.isInterlerp()) {
            linkInterlerp.step();
        }
        else {
            persistFunc.accept(linkInterlerp.getLinkObj(), batch);
        }
    }

    public void draw() {
        U linkObj = linkInterlerp.getLinkObj();
        Batchable linkBa = new Batchable((linkObj instanceof Pair pair) ? pair.getValue0() : linkObj);
        try {
            linkBa.draw(batch);
        } catch (BatchableException e) {
            throw new RuntimeException(e);
        }
    }
    public static <T,U> Pair<T,U> extractPair(Object obj) {
        return (Pair<T,U>) obj;
    }


}
