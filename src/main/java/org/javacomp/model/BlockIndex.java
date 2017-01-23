package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** Index of entities in a unnamed block of statements, such as if, while, or switch. */
public class BlockIndex implements EntityIndex {
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final EntityIndex parentIndex;

  public BlockIndex(EntityIndex parentIndex) {
    this.entities = HashMultimap.create();
    this.parentIndex = parentIndex;
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(entities.get(simpleName));
    builder.addAll(parentIndex.getEntitiesWithName(simpleName));
    return builder.build();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    for (Entity entity : entities.get(simpleName)) {
      if (entity.getKind() == entityKind) {
        return Optional.of(entity);
      }
    }
    return parentIndex.getEntityWithNameAndKind(simpleName, entityKind);
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(entities);
    builder.putAll(parentIndex.getAllEntities());
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    return ImmutableMultimap.of();
  }

  @Override
  public void addEntity(Entity entity) {
    entities.put(entity.getSimpleName(), entity);
  }

  @Override
  public Optional<EntityIndex> getParentIndex() {
    return Optional.of(parentIndex);
  }
}
