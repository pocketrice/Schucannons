package io.github.pocketrice.shared;

import java.util.function.Consumer;

public class Onceton<T> { // Similar to singleton, but only performs the action once. This is useful for use in per-tick updating
    Consumer<T> func;
    T obj;
    boolean hasExecuted;

    public Onceton(Consumer<T> func, T obj) {

    }
}
