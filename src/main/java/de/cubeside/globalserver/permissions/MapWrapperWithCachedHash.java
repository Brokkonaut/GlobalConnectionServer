package de.cubeside.globalserver.permissions;

import java.util.Map;

public class MapWrapperWithCachedHash<T extends Map<?, ?>> {
    private final T map;
    private int hashCode;

    public MapWrapperWithCachedHash(T map) {
        this.map = map;
        this.hashCode = map.hashCode();
    }

    public T getMap() {
        return map;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof MapWrapperWithCachedHash m && map.equals(m.map);
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
