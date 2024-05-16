package io.github.pocketrice.shared;

import lombok.Getter;

@Getter
public class UnidirInterlerper<T> extends Interlerper<T> {
    boolean isUnidirForward;

    public UnidirInterlerper(T start, T end) {
        this(start, end, EasingFunction.LINEAR, 0.2, true);
    }

    public UnidirInterlerper(T start, T end, EasingFunction easing, double ss, boolean isUniForward) {
        super(start, end, easing, ss);
        isUnidirForward = isUniForward;
    }

    public UnidirInterlerper(T start, T end, EasingFunction easing, long scount, boolean isUniForward) {
        super(start, end, easing, scount);
        isUnidirForward = isUniForward;
    }

    public UnidirInterlerper(Interlerper<T> interlerper, boolean isUniForward) {
        super(interlerper.getStartVal(), interlerper.getEndVal(), interlerper.getEasing(), interlerper.getStepSize());
        isUnidirForward = isUniForward;
    }

    @Override
    public UnidirInterlerper<T> from(T val) {
        return new UnidirInterlerper<>(super.from(val), this.isUnidirForward);
    }

    @Override
    public UnidirInterlerper<T> to(T val) {
        return new UnidirInterlerper<>(super.to(val), this.isUnidirForward);
    }

    @Override
    public UnidirInterlerper<T> type(EasingFunction e, double ss) {
        return new UnidirInterlerper<>(super.type(e, ss), this.isUnidirForward);
    }

    @Override
    public T interlerp(double t, EasingFunction easing) {
        return (isForward() == isUnidirForward) ? super.interlerp(t, easing) : getEndVal(); // Interlerp, otherwise do as if there was no interlerp
    }
}
