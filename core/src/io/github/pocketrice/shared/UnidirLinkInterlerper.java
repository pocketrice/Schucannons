package io.github.pocketrice.shared;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class UnidirLinkInterlerper<T,U> extends LinkInterlerper<T,U> {
    private boolean isUnidirForward;

    public UnidirLinkInterlerper(T start, T end) {
        this(start, end, EasingFunction.LINEAR, 0.2, true);
    }

    public UnidirLinkInterlerper(T start, T end, EasingFunction easing, double ss, boolean isUniForward) {
        super(start, end, easing, ss);
        isUnidirForward = isUniForward;
    }

    public UnidirLinkInterlerper(T start, T end, EasingFunction easing, long scount, boolean isUniForward) {
        super(start, end, easing, scount);
        isUnidirForward = isUniForward;
    }

    public UnidirLinkInterlerper(UnidirInterlerper<T> uinterlerp) {
        super(uinterlerp);
        isUnidirForward = uinterlerp.isUnidirForward();
    }

    public UnidirLinkInterlerper(LinkInterlerper<T,U> linkInterlerp, boolean isUniForward) {
        this(linkInterlerp.getStartVal(), linkInterlerp.getEndVal(), linkInterlerp.getEasing(), linkInterlerp.getStepSize(), isUniForward);
        this.linkObj(linkInterlerp.getLinkObj())
                .linkFunc(linkInterlerp.getLinkFunc())
                .preFunc(linkInterlerp.getPreFunc())
                .postFunc(linkInterlerp.getPostFunc());
    }

    @Override
    public void step() {
        double newParam = advanceParam(); // Moves param forward regardless of status

        if (isInterlerp() && isForward() == isUnidirForward) {
            double easedT = getEasing().apply(newParam);
            Consumer<U> preFunc = getPreFunc();
            Consumer<U> postFunc = getPostFunc();
            BiConsumer<Double, U> linkFunc = getLinkFunc();
            U linkObj = getLinkObj();


            if (getT() == ((isForward()) ? 0 : 1) && preFunc != null) preFunc.accept(linkObj);
            if (getT() == ((isForward()) ? 1 : 0) && postFunc != null) postFunc.accept(linkObj);

            if (isForward() && getT() >= 1 || getT() <= 0) {
                setInterlerp(false);
            } else {
                linkFunc.accept(easedT, linkObj);
            }
        }
    }
}
