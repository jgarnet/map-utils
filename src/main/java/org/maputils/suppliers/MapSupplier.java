package org.maputils.suppliers;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public enum MapSupplier {
    HASH_MAP(HashMap::new),
    LINKED_HASH_MAP(LinkedHashMap::new),
    TREE_MAP(size -> new TreeMap<>()),
    WEAK_HASH_MAP(WeakHashMap::new),
    IDENTITY_HASH_MAP(IdentityHashMap::new),
    CONCURRENT_HASH_MAP(ConcurrentHashMap::new);

    private final Function<Integer, Map<Object, Object>> supplier;

    MapSupplier(Function<Integer, Map<Object, Object>> supplier) {
        this.supplier = supplier;
    }

    public Function<Integer, Map<Object, Object>> getSupplier() {
        return supplier;
    }
}
