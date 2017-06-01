package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.List;
import java.util.Optional;
import org.javacomp.model.util.QualifiedNames;

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
      ClassEntity classEntity,
      Range<Integer> methodNamelRange) {
    super(simpleName, Entity.Kind.METHOD, qualifiers, methodNamelRange);
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

  /////////////// Other methods ////////////////

  public List<VariableEntity> getParameters() {
    return parameters;
  }

  public TypeReference getReturnType() {
    return returnType;
  }

  public ClassEntity getParentClass() {
    return classEntity;
  }

  @Override
  public String toString() {
    return "MethodEntity<"
        + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
        + ">";
  }
}
