package org.maputils;

import java.util.*;

/**
 * Provides helper methods to interact with Maps inspired by lodash
 */
@SuppressWarnings("rawtypes,unchecked")
public class MapUtils {

    private MapSupplier mapSupplier = MapSupplier.HASH_MAP;
    private ListSupplier listSupplier = ListSupplier.ARRAY_LIST;
    private SetSupplier setSupplier = SetSupplier.HASH_SET;

    public MapSupplier getMapSupplier() {
        return mapSupplier;
    }

    public void setMapSupplier(MapSupplier mapSupplier) {
        this.mapSupplier = mapSupplier;
    }

    public ListSupplier getListSupplier() {
        return listSupplier;
    }

    public void setListSupplier(ListSupplier listSupplier) {
        this.listSupplier = listSupplier;
    }

    public SetSupplier getSetSupplier() {
        return setSupplier;
    }

    public void setSetSupplier(SetSupplier setSupplier) {
        this.setSupplier = setSupplier;
    }

    public <T> Optional<T> read(Map<String, Object> map, String path) {
        if (map == null || path == null || path.trim().length() == 0) {
            return Optional.empty();
        }
        String[] nodes = path.split("\\.");
        Object current = map.get(nodes[0]);
        if (current == null) {
            return Optional.empty();
        }
        for (int i = 1; i < nodes.length; i++) {
            String node = nodes[i];
            // nested map
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(node);
            } else if (current instanceof List) {
                // nested collection
                if (!node.matches("^\\d+$")) {
                    // only allow numeric indexes to be accessible on Lists
                    return Optional.empty();
                }
                current = ((List<Object>) current).get(Integer.parseInt(node));
            }
            // todo: add support for other Collection types?
        }
        return (Optional<T>) Optional.of(current);
    }

    public void merge(Map<String, Object> target, Map<String, Object> source) {
        this.merge(target, source, null);
    }

    /**
     * Merges values from source object into target object.
     * Merges list items using collectionKeys which should provide the primary identifier for an Object in the given List.
     * @param target The object which values will be merged into.
     * @param source The object which values will be merged from.
     * @param collectionKeys Map which specifies the primary identifier for Objects in a Collection.
     */
    public void merge(Map<String, Object> target, Map<String, Object> source, Map<String, String> collectionKeys) {
        if (target != null && source != null) {
            for (Map.Entry<String, Object> entry : source.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                Object targetValue = target.get(key);
                if (!target.containsKey(key)) {
                    // target doesn't contain source key; simply add to target
                    target.put(key, value);
                } else if (value instanceof Map) {
                    if (targetValue instanceof Map) {
                        // target contains key and is a map; recursively merge values
                        this.merge((Map<String, Object>) targetValue, (Map<String, Object>) value, collectionKeys);
                    } else if (targetValue instanceof List) {
                        // target contains key and is a List
                        List<Object> result = this.mergeList((List<Object>) targetValue, (List<Object>) value, collectionKeys, key);
                        target.put(key, result);
                    } else {
                        // overwrite
                        target.put(key, value);
                    }
                } else if (value instanceof List && targetValue instanceof List) {
                    List<Object> result = this.mergeList((List<Object>) targetValue, (List<Object>) value, collectionKeys, key);
                    target.put(key, result);
                } else {
                    // overwrite
                    target.put(key, value);
                }
            }
        }
    }

    private List<Object> mergeList(List<Object> target, List<Object> source, Map<String, String> collectionKeys, String key) {
        // todo: refactor logic to maintain insertion order
        Object listValue = source.size() > 0 ? source.get(0) : null;
        Object targetValue = target.size() > 0 ? target.get(0) : null;
        if (listValue != null) {
            if (listValue instanceof Map && targetValue instanceof Map) {
                int targetSize = target.size();
                if (collectionKeys != null && collectionKeys.containsKey(key)) {
                    // we know how to account for merges in this instance; merge each Map
                    Map<String, Map> targetValues = new HashMap<>(targetSize);
                    String collectionKey = collectionKeys.get(key);
                    for (Object val : target) {
                        String currentKey = this.getCollectionKey((Map<String, Object>) val, collectionKey);
                        targetValues.put(currentKey, (Map) val);
                    }
                    for (Object val : source) {
                        Object result = this.read((Map<String, Object>) val, collectionKey).orElse(null);
                        String currentKey = result != null ? result.toString() : null;
                        if (targetValues.containsKey(currentKey)) {
                            this.merge((Map<String, Object>) targetValues.get(currentKey), (Map<String, Object>) val, collectionKeys);
                        } else {
                            targetValues.put(currentKey, (Map<String, Object>) val);
                        }
                    }
                    List<Object> list = this.listSupplier.getSupplier().apply(targetValues.size());
                    list.addAll(targetValues.values());
                    return list;
                } else {
                    return this.mergeUniqueListItems(target, source);
                }
            } else {
                return this.mergeUniqueListItems(target, source);
            }
        }
        return target;
    }

    private List<Object> mergeUniqueListItems(List<Object> target, List<Object> source) {
        // todo: refactor logic to maintain insertion order
        // we don't know how to account for merges; combine all values
        int targetSize = target.size();
        int sourceSize = source.size();
        Map<Integer, Object> uniqueValues = new HashMap<>(targetSize + sourceSize);
        for (Object val : target) {
            uniqueValues.put(val.hashCode(), val);
        }
        for (Object val : source) {
            uniqueValues.put(val.hashCode(), val);
        }
        List<Object> list = this.listSupplier.getSupplier().apply(uniqueValues.size());
        list.addAll(uniqueValues.values());
        return list;
    }

    private String getCollectionKey(Map<String, Object> map, String collectionKey) {
        if (collectionKey.contains(",")) {
            // support composite keys
            StringBuilder sb = new StringBuilder();
            String[] parts = collectionKey.split(",");
            for (String part : parts) {
                Optional<Object> result = this.read(map, part);
                result.ifPresent(sb::append);
            }
            return this.read(map, sb.toString()).map(Object::toString).orElse(null);
        }
        return this.read(map, collectionKey).map(Object::toString).orElse(null);
    }

    public Map<Object, Object> cloneDeep(Map<Object, Object> map) {
        Map<Object, Object> clone = (Map<Object, Object>) this.mapSupplier.getSupplier().apply(map.size());
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = entry.getKey();
            Object value = entry.getValue();
            clone.put(key, this.cloneDeepValue(value));
        }
        return clone;
    }

    private <T extends Map> Object cloneDeepValue(Object value) {
        if (value instanceof Map) {
            return this.cloneDeep((Map<Object, Object>) value);
        } else if (value instanceof List) {
            List<Object> list = this.listSupplier.getSupplier().apply(((List) value).size());
            this.cloneCollection(list, (List) value);
            return list;
        } else if (value instanceof Set) {
            Set<Object> set = this.setSupplier.getSupplier().apply(((Set) value).size());
            this.cloneCollection(set, (Set) value);
            return set;
        }
        // todo: account for non-primitive objects?
        return value;
    }

    private void cloneCollection(Collection<Object> target, Collection<Object> source) {
        for (Object subValue : source) {
            if (subValue instanceof Map) {
                target.add(this.cloneDeep((Map<Object, Object>) subValue));
            } else {
                target.add(this.cloneDeepValue(subValue));
            }
        }
    }

}
