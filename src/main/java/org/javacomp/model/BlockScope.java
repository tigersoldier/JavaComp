package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
