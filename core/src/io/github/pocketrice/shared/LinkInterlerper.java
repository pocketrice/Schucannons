package io.github.pocketrice.shared;

import lombok.Getter;
import lombok.Setter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Getter
public class LinkInterlerper<T,U> extends Interlerper<T>{
    private BiConsumer<Double, U> linkFunc;
    private Consumer<U> preFunc, postFunc;
    @Setter
    private U linkObj;


    public LinkInterlerper(T start, T end) {
        this(start, end, EasingFunction.LINEAR, 0.1);
    }

    public LinkInterlerper(T start, T end, EasingFunction e, double ss) {
        super(start, end, e, ss);
    }


    public LinkInterlerper<T,U> linkFunc(BiConsumer<Double, U> func) {
        linkFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> preFunc(Consumer<U> func) {
        preFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> postFunc(Consumer<U> func) {
        postFunc = func;
        return this;
    }

    public LinkInterlerper<T,U> linkObj(U obj) {
        linkObj = obj;
        return this;
    }


    // A bulkier variant of advance() that can run a func on a separate obj (usually parent) for each advance. Useful for frame-based render().
    public void step() {
        if (isInterlerp()) {
            double easedT = getEasing().apply(advanceParam());
            if (getT() == 0 && preFunc != null) preFunc.accept(linkObj);
            if (getT() == 1 && postFunc != null) postFunc.accept(linkObj);

            if (isForward() && getT() >= 1 || getT() <= 0) {
                setInterlerp(false);
            } else {
                linkFunc.accept(easedT, linkObj);
            }
        }
    }
}
