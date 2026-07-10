package androidx.collection;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ArrayMap<K, V> implements Map<K, V> {
    public ArrayMap() {

    }

    public ArrayMap(int capacity) {

    }

    @Override
    public void putAll(@NonNull Map<? extends K, ? extends V> map) {

    }

    @NonNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    @NonNull
    @Override
    public Set<K> keySet() {
        return null;
    }

    @NonNull
    @Override
    public Collection<V> values() {
        return null;
    }

    @Override
    public void clear() {

    }

    @Override
    public boolean containsKey(@Nullable Object key) {
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        return false;
    }

    @Nullable
    @Override
    public V get(Object key) {
        return null;
    }

    @Nullable
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        return null;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        return null;
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        return null;
    }

    @Nullable
    @Override
    public V remove(Object key) {
        return null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        return false;
    }

    @Nullable
    @Override
    public V replace(K key, V value) {
        return null;
    }

    @Override
    public boolean replace(K key, V oldValue, V newValue) {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }
}