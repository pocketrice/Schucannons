package io.github.pocketrice.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.function.BiConsumer;

@Getter
public class LinkInterlerper<T,U> extends Interlerper<T>{
    @Setter
    private BiConsumer<Double, U> linkFunc;
    @Setter
    private U linkObj;
    private boolean isInterlerp, isForward;

    public LinkInterlerper(T start, T end) {
        super(start, end);
    }

    public LinkInterlerper<T,U> linkFunc(BiConsumer<Double, U> func) {
        linkFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> linkObj(U obj) {
        linkObj = obj;
        return this;
    }


    // A bulkier variant of advance() that can run a func on a separate obj (usually parent) for each advance. Useful for frame-based render().
    public void step(double stepSize, EasingFunction easing) {
        double easedT = easing.apply(advanceParam(stepSize, isForward));
        if (isForward && getT() >= 1 || getT() <= 0) {
            isInterlerp = false;
        } else {
            linkFunc.accept(easedT, linkObj);
        }
    }

    public void setInterlerpStatus(boolean isReady) {
        isInterlerp = isReady;
    }

    public void setDirection(boolean isFwd) {
        isForward = isFwd;
    }
}
