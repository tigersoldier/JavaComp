package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;

/** Scope of entities declared in a method. */
public class MethodScope implements EntityScope {
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final ClassEntity classEntity;

  public MethodScope(ClassEntity classEntity) {
    this.entities = HashMultimap.create();
    this.classEntity = classEntity;
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(entities.get(simpleName));
    builder.addAll(classEntity.getEntitiesWithName(simpleName));
    // TODO: distinguish between static method and instance method
    return builder.build();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    for (Entity entity : entities.get(simpleName)) {
      if (entity.getKind() == entityKind) {
        return Optional.of(entity);
      }
    }
    // TODO: distinguish between static method and instance method
    return classEntity.getEntityWithNameAndKind(simpleName, entityKind);
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(entities);
    builder.putAll(classEntity.getAllEntities());
    // TODO: distinguish between static method and instance method
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
    return Optional.of(classEntity);
  }

  public ClassEntity getParentClass() {
    return classEntity;
  }
}
