package org.maputils.suppliers;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

public enum SetSupplier {
    HASH_SET(HashSet::new),
    LINKED_HASH_SET(LinkedHashSet::new),
    TREE_SET(size -> new TreeSet<>());

    private final Function<Integer, Set<Object>> supplier;

    SetSupplier(Function<Integer, Set<Object>> supplier) {
        this.supplier = supplier;
    }

    public Function<Integer, Set<Object>> getSupplier() {
        return supplier;
    }
}
