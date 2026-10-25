package com.everythingengine.core.universe;

import com.everythingengine.core.entity.Entity;
import com.everythingengine.core.relation.Relation;
import com.everythingengine.core.relation.RelationType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Owns registered entities and relations for one universe.
 */
public final class Universe {
    private final Map<UUID, Entity> entities = new HashMap<>();
    private final Map<UUID, Relation> relations = new HashMap<>();
    private final Map<UUID, Set<UUID>> relationIdsBySource = new HashMap<>();
    private final Map<UUID, Set<UUID>> relationIdsByTarget = new HashMap<>();
    private final Map<RelationType, Set<UUID>> relationIdsByType = new HashMap<>();

    public void registerEntity(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        if (entities.putIfAbsent(entity.id(), entity) != null) {
            throw new IllegalArgumentException("An entity with UUID " + entity.id() + " is already registered");
        }
    }

    public Optional<Entity> findEntity(UUID id) {
        return Optional.ofNullable(entities.get(Objects.requireNonNull(id, "id")));
    }

    public boolean containsEntity(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        return entities.get(entity.id()) == entity;
    }

    public boolean removeEntity(UUID id) {
        Objects.requireNonNull(id, "id");
        if (entities.remove(id) == null) {
            return false;
        }
        Set<UUID> connectedRelationIds = new LinkedHashSet<>();
        addAll(connectedRelationIds, relationIdsBySource.get(id));
        addAll(connectedRelationIds, relationIdsByTarget.get(id));
        connectedRelationIds.forEach(this::removeRelation);
        return true;
    }

    public List<Entity> entities() {
        return List.copyOf(entities.values());
    }

    public <T extends Entity> List<T> entitiesOfType(Class<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return entities.values().stream()
                .filter(entityType::isInstance)
                .map(entityType::cast)
                .toList();
    }

    public void addRelation(Relation relation) {
        Objects.requireNonNull(relation, "relation");
        requireRegisteredEndpoint(relation.source());
        requireRegisteredEndpoint(relation.target());
        if (relations.putIfAbsent(relation.id(), relation) != null) {
            throw new IllegalArgumentException("A relation with UUID " + relation.id() + " is already registered");
        }
        addIndex(relationIdsBySource, relation.source().id(), relation.id());
        addIndex(relationIdsByTarget, relation.target().id(), relation.id());
        addIndex(relationIdsByType, relation.type(), relation.id());
    }

    public Optional<Relation> findRelation(UUID id) {
        return Optional.ofNullable(relations.get(Objects.requireNonNull(id, "id")));
    }

    public List<Relation> relations() {
        return List.copyOf(relations.values());
    }

    public List<Relation> relationsFrom(Entity source) {
        return relationsFor(source, relationIdsBySource);
    }

    public List<Relation> relationsTo(Entity target) {
        return relationsFor(target, relationIdsByTarget);
    }

    public List<Relation> relationsOf(Entity entity) {
        Objects.requireNonNull(entity, "entity");
        Set<UUID> ids = new LinkedHashSet<>();
        addAll(ids, relationIdsBySource.get(entity.id()));
        addAll(ids, relationIdsByTarget.get(entity.id()));
        return relationsForIds(ids);
    }

    public List<Relation> relationsOfType(RelationType type) {
        Objects.requireNonNull(type, "type");
        return relationsForIds(relationIdsByType.get(type));
    }

    public boolean removeRelation(UUID id) {
        Objects.requireNonNull(id, "id");
        Relation relation = relations.remove(id);
        if (relation == null) {
            return false;
        }
        removeIndex(relationIdsBySource, relation.source().id(), id);
        removeIndex(relationIdsByTarget, relation.target().id(), id);
        removeIndex(relationIdsByType, relation.type(), id);
        return true;
    }

    private void requireRegisteredEndpoint(Entity endpoint) {
        if (entities.get(endpoint.id()) != endpoint) {
            throw new IllegalStateException(
                    "Relation endpoint " + endpoint.id() + " is not registered in this universe");
        }
    }

    private <K> void addIndex(Map<K, Set<UUID>> index, K key, UUID relationId) {
        index.computeIfAbsent(key, ignored -> new LinkedHashSet<>()).add(relationId);
    }

    private <K> void removeIndex(Map<K, Set<UUID>> index, K key, UUID relationId) {
        Set<UUID> ids = index.get(key);
        if (ids != null) {
            ids.remove(relationId);
            if (ids.isEmpty()) {
                index.remove(key);
            }
        }
    }

    private <K> List<Relation> relationsFor(Entity entity, Map<K, Set<UUID>> index) {
        Objects.requireNonNull(entity, "entity");
        return relationsForIds(index.get(entity.id()));
    }

    private List<Relation> relationsForIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Relation> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            Relation relation = relations.get(id);
            if (relation != null) {
                result.add(relation);
            }
        }
        return List.copyOf(result);
    }

    private void addAll(Set<UUID> destination, Set<UUID> source) {
        if (source != null) {
            destination.addAll(source);
        }
    }
}
