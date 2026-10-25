package com.everythingengine.core.spatial;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Rebuildable linear spatial index. Each query is O(n), appropriate for populations of thousands.
 * Position updates become visible after upsert or rebuild; the source objects remain authoritative.
 */
public final class SpatialIndex<T> {
    private final CoordinateSpace space;
    private Map<UUID, SpatialItem<T>> items = new HashMap<>();

    public SpatialIndex(CoordinateSpace space) {
        this.space = Objects.requireNonNull(space, "space");
    }

    public void rebuild(Collection<SpatialItem<T>> entries) {
        Objects.requireNonNull(entries, "entries");
        Map<UUID, SpatialItem<T>> replacement = new HashMap<>();
        for (SpatialItem<T> item : entries) {
            requireSpace(item.position());
            if (replacement.putIfAbsent(item.id(), item) != null) {
                throw new IllegalArgumentException("Duplicate spatial item id: " + item.id());
            }
        }
        items = replacement;
    }

    public void upsert(SpatialItem<T> item) {
        Objects.requireNonNull(item, "item");
        requireSpace(item.position());
        items.put(item.id(), item);
    }

    public boolean remove(UUID id) {
        return items.remove(Objects.requireNonNull(id, "id")) != null;
    }

    public int size() {
        return items.size();
    }

    public Optional<SpatialItem<T>> find(UUID id) {
        return Optional.ofNullable(items.get(Objects.requireNonNull(id, "id")));
    }

    public List<SpatialItem<T>> withinRadius(Position3D center, double radiusParsecs) {
        Objects.requireNonNull(center, "center");
        requireSpace(center);
        if (!Double.isFinite(radiusParsecs) || radiusParsecs < 0) {
            throw new IllegalArgumentException("Radius must be finite and non-negative");
        }
        return orderedItems().stream()
                .filter(item -> item.position().distanceTo(center) <= radiusParsecs)
                .toList();
    }

    public List<SpatialItem<T>> withinBounds(SpatialBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        requireSpace(bounds.center());
        return orderedItems().stream().filter(item -> bounds.contains(item.position())).toList();
    }

    public List<SpatialItem<T>> nearest(Position3D position, int count) {
        Objects.requireNonNull(position, "position");
        requireSpace(position);
        if (count < 0) {
            throw new IllegalArgumentException("Nearest item count must not be negative");
        }
        return orderedItems().stream()
                .sorted(Comparator.comparingDouble((SpatialItem<T> item) -> item.position().distanceTo(position))
                        .thenComparing(SpatialItem::id))
                .limit(count)
                .toList();
    }

    private List<SpatialItem<T>> orderedItems() {
        return items.values().stream().sorted(Comparator.comparing(SpatialItem::id)).toList();
    }

    private void requireSpace(Position3D position) {
        if (!space.equals(position.space())) {
            throw new IllegalArgumentException("Position does not belong to this spatial index's coordinate space");
        }
    }

}
