package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** An scope containing no entity. */
public class EmptyScope implements EntityScope {
  public static final EmptyScope INSTANCE = new EmptyScope();

  private EmptyScope() {}

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    return ImmutableList.of();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    return Optional.absent();
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    return ImmutableMultimap.of();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    return ImmutableMultimap.of();
  }

  @Override
  public void addEntity(Entity entity) {
    throw new UnsupportedOperationException("No entity is allowed to be added to a EmptyScope.");
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.absent();
  }
}
