package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;

/** Scope of entities in a unnamed block of statements, such as if, while, or switch. */
public class BlockScope implements EntityScope {
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final EntityScope parentScope;

  public BlockScope(EntityScope parentScope) {
    this.entities = HashMultimap.create();
    this.parentScope = parentScope;
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(entities.get(simpleName));
    builder.addAll(parentScope.getEntitiesWithName(simpleName));
    return builder.build();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    for (Entity entity : entities.get(simpleName)) {
      if (entity.getKind() == entityKind) {
        return Optional.of(entity);
      }
    }
    return parentScope.getEntityWithNameAndKind(simpleName, entityKind);
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(entities);
    builder.putAll(parentScope.getAllEntities());
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    return ImmutableMultimap.copyOf(entities);
  }

  @Override
  public void addEntity(Entity entity) {
    entities.put(entity.getSimpleName(), entity);
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.of(parentScope);
  }
}
