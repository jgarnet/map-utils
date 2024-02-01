package org.maputils;

import org.maputils.suppliers.ListSupplier;
import org.maputils.suppliers.MapSupplier;
import org.maputils.suppliers.SetSupplier;

import java.util.*;

/**
 * Provides helper methods to interact with Maps inspired by lodash
 */
@SuppressWarnings("rawtypes,unchecked")
public class MapUtils {

    /**
     * Determines what Map implementation will be used when adding Map fields.
     */
    private MapSupplier mapSupplier = MapSupplier.HASH_MAP;
    /**
     * Determines what List implementation will be used when adding List fields.
     */
    private ListSupplier listSupplier = ListSupplier.ARRAY_LIST;
    /**
     * Determines what Set implementation will be used when adding Set fields.
     */
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

    /**
     * Extract data from a Map given a dot-notation path
     * @param map The target Map
     * @param path Dot-notation path
     * @return Optional value extracted from the target Map
     */
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

    /**
     * Merges values from source object into target object.
     * Merges list items using collectionKeys which should provide the primary identifier for an Object in the given List.
     * @param target The object which values will be merged into.
     * @param source The object which values will be merged from.
     */
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
                    target.put(key, value);
                } else if (value instanceof Map) {
                    if (targetValue instanceof Map) {
                        this.merge((Map<String, Object>) targetValue, (Map<String, Object>) value, collectionKeys);
                    } else {
                        target.put(key, value);
                    }
                } else if (this.isCollection(value) && this.isCollection(targetValue)) {
                    Collection<Object> result = this.mergeCollection(
                            (Collection<Object>) targetValue,
                            (Collection<Object>) value,
                            collectionKeys,
                            key
                    );
                    target.put(key, result);
                } else {
                    target.put(key, value);
                }
            }
        }
    }

    private boolean isCollection(Object target) {
        return target instanceof List || target instanceof Set;
    }

    private Collection<Object> mergeCollection(Collection<Object> target, Collection<Object> source, Map<String, String> collectionKeys, String key) {
        Object listValue = source.size() > 0 ? source.iterator().next() : null;
        Object targetValue = target.size() > 0 ? target.iterator().next() : null;
        if (listValue != null) {
            if (listValue instanceof Map && targetValue instanceof Map) {
                int targetSize = target.size();
                if (collectionKeys != null && collectionKeys.containsKey(key)) {
                    // we know how to account for merges in this instance; merge each Map
                    Map<String, Map> targetValues = new LinkedHashMap<>(targetSize);
                    String collectionKey = collectionKeys.get(key);
                    for (Object val : target) {
                        String currentKey = this.getCollectionKey((Map<String, Object>) val, collectionKey);
                        targetValues.put(currentKey, (Map) val);
                    }
                    for (Object val : source) {
                        String currentKey = this.getCollectionKey((Map<String, Object>) val, collectionKey);
                        // if an object with the same identifier exists already, merge the two values
                        if (targetValues.containsKey(currentKey)) {
                            this.merge(
                                    (Map<String, Object>) targetValues.get(currentKey),
                                    (Map<String, Object>) val,
                                    collectionKeys
                            );
                        } else {
                            // otherwise, add this value to the target Map
                            targetValues.put(currentKey, (Map<String, Object>) val);
                        }
                    }
                    return this.mergeCollectionItems(target, targetValues.values());
                } else {
                    return this.mergeUniqueCollectionItems(target, source);
                }
            } else {
                return this.mergeUniqueCollectionItems(target, source);
            }
        }
        return target;
    }

    private Collection<Object> mergeUniqueCollectionItems(Collection<Object> target, Collection<Object> source) {
        // we don't know how to account for merges; combine all values
        int targetSize = target.size();
        int sourceSize = source.size();
        Map<Integer, Object> uniqueValues = new LinkedHashMap<>(targetSize + sourceSize);
        for (Object val : target) {
            uniqueValues.put(val.hashCode(), val);
        }
        for (Object val : source) {
            uniqueValues.put(val.hashCode(), val);
        }
        return this.mergeCollectionItems(target, uniqueValues.values());
    }

    private Collection<Object> mergeCollectionItems(Collection<Object> target, Collection<?> values) {
        Collection<Object> collection;
        if (target instanceof Set) {
            collection = this.setSupplier.getSupplier().apply(values.size());
        } else {
            collection = this.listSupplier.getSupplier().apply(values.size());
        }
        collection.addAll(values);
        return collection;
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

    /**
     * Returns a deep clone of a Map
     * @param map The Map being cloned
     * @return A deep cloned Map
     */
    public Map<Object, Object> cloneDeep(Map<Object, Object> map) {
        Map<Object, Object> clone = this.mapSupplier.getSupplier().apply(map.size());
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

    /**
     * Assigns values to a target Map from a source Map.
     * If target contain overlapping keys with source, the values will be overwritten.
     * @param target The target Map values are being assigned to.
     * @param source The source Map values are being assigned from.
     * @param keys The paths to assign. Left-hand side contains paths to assign to target, right-hand side contains paths to read from source.
     * @param assignPaths Toggles whether keys can contain nested paths.
     */
    public void assign(Map target, Map source, Map<String, String> keys, boolean assignPaths) {
        if (target != null && source != null && keys != null) {
            for (Map.Entry<String, String> entry : keys.entrySet()) {
                Object value = this.read(source, entry.getValue()).orElse(null);
                if (value != null) {
                    if (assignPaths && entry.getKey().contains(".")) {
                        String[] parts = entry.getKey().split("\\.");
                        Map<String, Object> currentTarget = target;
                        for (int i = 0; i < parts.length - 1; i++) {
                            String part = parts[i];
                            // todo: account for Collections
                            currentTarget = currentTarget.containsKey(part) ?
                                    (Map) currentTarget.get(part) :
                                    this.addNode(currentTarget, part);
                        }
                        currentTarget.put(parts[parts.length - 1], value);
                    } else {
                        target.put(entry.getKey(), value);
                    }
                }
            }
        }
    }

    /**
     * Adds a node to a Map given a dot-notation path.
     * All nodes are treated as Maps, with no support for Collections.
     * @param map The target Map
     * @param path Dot-notation path.
     * @return The final path node being added.
     */
    public Map addNode(Map map, String path) {
        if (map == null) {
            return null;
        }
        String[] nodes = path.split("\\.");
        Map current = map;
        for (String node : nodes) {
            if (!current.containsKey(node)) {
                Map nodeMap = this.mapSupplier.getSupplier().apply(1);
                current.put(node, nodeMap);
            }
            current = (Map) current.get(node);
        }
        return current;
    }

}
