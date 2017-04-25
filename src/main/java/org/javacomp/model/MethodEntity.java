package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Represents a method. */
public class MethodEntity extends Entity implements EntityScope {
  private final TypeReference returnType;
  private final List<VariableEntity> parameters;
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final ClassEntity classEntity;

  public MethodEntity(
      String simpleName,
      List<String> qualifiers,
      TypeReference returnType,
      List<VariableEntity> parameters,
      ClassEntity classEntity) {
    super(simpleName, Entity.Kind.METHOD, qualifiers);
    this.returnType = returnType;
    this.parameters = ImmutableList.copyOf(parameters);
    this.entities = HashMultimap.create();
    this.classEntity = classEntity;
  }

  /////////////// Entity methods ////////////////

  @Override
  public MethodEntity getChildScope() {
    return this;
  }

  /////////////// EntityScope methods ///////////////

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(entities.get(simpleName));
    builder.addAll(classEntity.getEntitiesWithName(simpleName));
    builder.addAll(parameters);
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
    if (entityKind == Entity.Kind.VARIABLE) {
      for (VariableEntity parameter : parameters) {
        if (Objects.equals(parameter.getSimpleName(), simpleName)) {
          return Optional.of(parameter);
        }
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
    for (VariableEntity parameter : parameters) {
      builder.put(parameter.getSimpleName(), parameter);
    }
    // TODO: distinguish between static method and instance method
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(entities);
    for (VariableEntity parameter : parameters) {
      builder.put(parameter.getSimpleName(), parameter);
    }
    return builder.build();
  }

  @Override
  public void addEntity(Entity entity) {
    entities.put(entity.getSimpleName(), entity);
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.of(classEntity);
  }

  /////////////// Non overriding methods ////////////////

  public List<VariableEntity> getParameters() {
    return parameters;
  }

  public TypeReference getReturnType() {
    return returnType;
  }

  public ClassEntity getParentClass() {
    return classEntity;
  }
}
