package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.javacomp.model.util.QualifiedNames;

/** Represents a class, interface, enum, or annotation. */
public class ClassEntity extends Entity implements EntityScope {
  public static final Set<Entity.Kind> ALLOWED_KINDS =
      EnumSet.of(
          Entity.Kind.CLASS, Entity.Kind.INTERFACE, Entity.Kind.ANNOTATION, Entity.Kind.ENUM);

  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  private final EntityScope parentScope;
  private final Optional<TypeReference> superClass;
  private final ImmutableList<TypeReference> interfaces;
  private final Map<String, ClassEntity> innerClasses;

  public ClassEntity(
      String simpleName,
      Entity.Kind kind,
      List<String> qualifiers,
      EntityScope parentScope,
      Optional<TypeReference> superClass,
      ImmutableList<TypeReference> interfaces) {
    super(simpleName, kind, qualifiers);
    checkArgument(
        ALLOWED_KINDS.contains(kind),
        "Invalid entity kind %s, allowed kinds are %s",
        kind,
        ALLOWED_KINDS);
    this.entities = HashMultimap.create();
    this.parentScope = parentScope;
    this.superClass = superClass;
    this.interfaces = ImmutableList.copyOf(interfaces);
    this.innerClasses = new HashMap<>();
  }

  @Override
  public ClassEntity getChildScope() {
    return this;
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    // TODO: check imports.
    // TODO: check super class and interfaces
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
    // TODO: check imports.
    // TODO: check super class and interfaces
    return parentScope.getEntityWithNameAndKind(simpleName, entityKind);
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(entities).putAll(innerClasses.entrySet()).putAll(parentScope.getAllEntities());
    // TODO: check imports.
    // TODO: check super class and interfaces
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    return builder.putAll(entities).putAll(innerClasses.entrySet()).build();
  }

  @Override
  public void addEntity(Entity entity) {
    if (entity instanceof ClassEntity) {
      innerClasses.put(entity.getSimpleName(), (ClassEntity) entity);
    } else {
      entities.put(entity.getSimpleName(), entity);
    }
  }

  public ImmutableList<TypeReference> getInterfaces() {
    return interfaces;
  }

  public Optional<TypeReference> getSuperClass() {
    return superClass;
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.of(parentScope);
  }

  public Map<String, ClassEntity> getInnerClasses() {
    return ImmutableMap.copyOf(innerClasses);
  }

  @Override
  public String toString() {
    return "ClassEntity<"
        + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
        + ">";
  }
}
