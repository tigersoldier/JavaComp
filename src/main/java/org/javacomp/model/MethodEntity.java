package org.javacomp.model;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.HashMultimap;
import java.util.List;
import java.util.Optional;

/** Represents a method. */
public class MethodEntity extends Entity implements EntityScope {
  private final TypeReference returnType;
  private final List<Parameter> parameters;
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final ClassEntity classEntity;

  public MethodEntity(
      String simpleName,
      List<String> qualifiers,
      TypeReference returnType,
      List<Parameter> parameters,
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

  /////////////// Non overriding methods ////////////////

  public List<Parameter> getParameters() {
    return parameters;
  }

  public TypeReference getReturnType() {
    return returnType;
  }

  public ClassEntity getParentClass() {
    return classEntity;
  }

  @AutoValue
  public abstract static class Parameter {
    public abstract TypeReference getType();

    public abstract String getName();

    public static Parameter create(TypeReference type, String name) {
      return new AutoValue_MethodEntity_Parameter(type, name);
    }
  }
}
