package io.github.pocketrice.shared;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EvictingList<E> extends ArrayList<E> { // Inspired by Guava implementation
    int maxSize;

    public EvictingList(int size) {
        super();
        maxSize = size;
    }

    public List<E> evict() { // circular fifo queue-style
        List<E> evicted = new ArrayList<>();

        while (this.size() > maxSize) {
            evicted.add(this.get(0));
        }

        return evicted;
    }

    @Override
    public boolean add(E e) {
        boolean res = super.add(e);
        evict();
        return res;
    }

    @Override
    public void add(int index, E element) {
        super.add(index, element);
        evict();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean res = super.addAll(c);
        evict();
        return res;
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> c) {
        boolean res = super.addAll(index, c);
        evict();
        return res;
    }

//    @Override
//    public boolean addAll(int index, E... e) -> due to https://stackoverflow.com/questions/51991795/overriding-varargs-in-java can't override varargs. So DON'T USE THIS METHOD!

    @Override
    public boolean equals(Object o) { // Same equals(), just checking maxSize too.
        var other = (EvictingList<E>) o;
        return super.equals(o) && this.maxSize == other.maxSize;
    }
}
