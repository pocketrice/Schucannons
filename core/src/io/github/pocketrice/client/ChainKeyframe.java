package io.github.pocketrice.client;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import io.github.pocketrice.client.ui.BatchGroup;
import io.github.pocketrice.client.ui.Batchable;
import io.github.pocketrice.client.ui.BatchableException;
import io.github.pocketrice.shared.LinkInterlerper;
import org.javatuples.Pair;

import java.util.function.BiConsumer;

public class ChainKeyframe<U> {
    LinkInterlerper<?, U> linkInterlerp;
    BiConsumer<U, SpriteBatch> persistFunc;
    SpriteBatch batch;
    BatchGroup baGroup;

    // Assume LinkInterlerper is of type <interpObj ∋ interpObj.isBatchable, Pair<linkObj, SpriteBatch>>
    public ChainKeyframe(LinkInterlerper<?, Pair<U, SpriteBatch>> li) {
        this((LinkInterlerper<?, U>) li, li.getLinkObj().getValue1(), (pair, batch) -> {
            U targetVal = flattenLinkPair(pair);
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

    // For compat with BatchGroup
    public ChainKeyframe(LinkInterlerper<?, U> li, BatchGroup bg) {
        linkInterlerp = li;
        linkInterlerp.setInterlerp(true);
        baGroup = bg;
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
            if (batch != null) { // Only apply persistFunc (which requires batch) if not using BatchGroup.
                persistFunc.accept(linkInterlerp.getLinkObj(), batch);
            }
        }
    }

    public void draw() {
        if (batch != null) {
            U linkObj = linkInterlerp.getLinkObj();
            Batchable linkBa = new Batchable((linkObj instanceof Pair pair) ? pair.getValue0() : linkObj);
            try {
                linkBa.draw(batch);
            } catch (BatchableException e) {
                throw new RuntimeException(e);
            }
        } else {
            baGroup.enable(flattenLinkPair(linkInterlerp.getLinkObj()));
        }
    }
    // CKF-specific helper method for flattening Pair<U, SpriteBatch> into U if such exists (cannot guarantee U otherwise).
    public static <U> U flattenLinkPair(Object obj) {
        return (U) ((obj instanceof Pair linkPair && linkPair.getValue1() instanceof SpriteBatch) ? linkPair.getValue0() : obj);
    }
}
