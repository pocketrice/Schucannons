package io.github.pocketrice.client;

import io.github.pocketrice.shared.Interval;
import org.javatuples.Triplet;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.function.Consumer;

public class IntervalBuffer {
    Queue<Triplet<Object, Consumer<Object>, Interval>> buffer;

    public IntervalBuffer() {
        buffer = new ArrayDeque<>();
    }

    public void enqueue(Object funcObj, Consumer<Object> func, Interval intv) {
        buffer.add(Triplet.with(funcObj, func, intv));
    }

    public Triplet<Object, Consumer<Object>, Interval> dequeue() {
        return buffer.remove();
    }

    public void gracefulClear() {
        buffer.forEach(i -> i.getValue1().accept(i.getValue0()));
        clear();
    }

    public void clear() {
        buffer.clear();
    }


    public void update() {
        Triplet<Object, Consumer<Object>, Interval> item = buffer.peek();
        if (item != null) {
            Interval interval = item.getValue2();
            Consumer<Object> func = item.getValue1();
            Object funcObj = item.getValue0();

            if (interval != null && !interval.observe()) {
                func.accept(funcObj);
                buffer.remove();
            }
        }
    }
}
