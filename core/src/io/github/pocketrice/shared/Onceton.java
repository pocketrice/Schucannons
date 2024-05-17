package io.github.pocketrice.shared;

import java.util.function.Consumer;

public class Onceton<T> { // Similar to singleton, but only performs the action once. Think of it like a temporal singleton... this is useful for use in per-tick updating :D
    Consumer<T> func;
    T obj;
    boolean hasExecuted;

    public Onceton(Consumer<T> func, T obj) {
        this.func = func;
        this.obj = obj;
        hasExecuted = false;
    }

    public boolean execute() {
        boolean execState = hasExecuted; // Store previous state
        if (!hasExecuted) {
            func.accept(obj);
            hasExecuted = true;
        }

        return execState;
    }

    public void reset() {
        hasExecuted = false;
    }
}
