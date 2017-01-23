package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;

/** An index containing no entity. */
public class LeafIndex implements EntityIndex {
  public static final LeafIndex INSTANCE = new LeafIndex();

  private LeafIndex() {}

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
    throw new UnsupportedOperationException("No entity is allowed to be added to a LeafIndex.");
  }

  @Override
  public Optional<EntityIndex> getParentIndex() {
    return Optional.absent();
  }
}
