package org.maputils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public enum ListSupplier {
    ARRAY_LIST(ArrayList::new),
    LINKED_LIST(size -> new LinkedList<>()),
    COPY_ON_WRITE_ARRAY_LIST(size -> new CopyOnWriteArrayList<>());

    private final Function<Integer, List<Object>> supplier;

    ListSupplier(Function<Integer, List<Object>> supplier) {
        this.supplier = supplier;
    }

    public Function<Integer, List<Object>> getSupplier() {
        return supplier;
    }
}
