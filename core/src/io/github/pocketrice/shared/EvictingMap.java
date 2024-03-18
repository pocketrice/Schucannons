package io.github.pocketrice.shared;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public class EvictingMap<K,V> extends LinkedHashMap<K,V> { // Inspired by Guava implementation
    int maxSize;

    public EvictingMap(int size) {
        super();
        maxSize = size;
    }

    public List<Map.Entry<K,V>> evict() { // circular fifo queue-style
        List<Map.Entry<K,V>> evicted = new ArrayList<>();
        for (Map.Entry<K,V> entry : this.entrySet()) { // only way to iterate eldest -> youngest. Hence, while is impossible since it's a set...
            if (this.size() - evicted.size() > maxSize) {
                evicted.add(entry);
            }
        }

        evicted.forEach(e -> this.remove(e.getKey())); // Cannot remove within forloop since it causes ConcurrentModificationException

        return evicted;
    }

    public int indexOf(K key) {
        int i = 0, keyIndex = -1;

        for (K mapKey : this.keySet()) {
            if (mapKey.equals(key)) keyIndex = i;
            i++;
        }

        return keyIndex;
    }

    public K keyAt(int index) {
        int i = 0;
        K key = null;

        for (K mapKey : this.keySet()) {
            if (i == index) key = mapKey;
            i++;
        }

        return key;
    }

    public V valueAt(int index) {
        return this.get(keyAt(index));
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V res = super.put(key, value);
        evict();
        return res;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> m) {
        super.putAll(m);
        evict();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappedFunc) {
        V res = super.compute(key, remappedFunc);
        evict();
        return res;
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunc) {
        V res = super.computeIfAbsent(key, mappingFunc);
        evict();
        return res;
    }

    @Override
    public V merge(K key, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunc) {
        V res = super.merge(key, value, remappingFunc);
        evict();
        return res;
    }

    @Override
    public boolean equals(Object o) { // Same equals(), just checking maxSize too.
        var other = (EvictingMap<K,V>) o;
        return super.equals(o) && this.maxSize == other.maxSize;
    }
}
