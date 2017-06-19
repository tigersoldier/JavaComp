package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.javacomp.model.util.QualifiedNames;

/** Represents a class, interface, enum, or annotation. */
public class ClassEntity extends Entity implements EntityScope {
  public static final Set<Entity.Kind> ALLOWED_KINDS =
      EnumSet.of(
          Entity.Kind.CLASS, Entity.Kind.INTERFACE, Entity.Kind.ANNOTATION, Entity.Kind.ENUM);

  // Map of simple names -> fields.
  private final Map<String, VariableEntity> fields;
  // Map of simple names -> methods.
  private final Multimap<String, MethodEntity> methods;
  private final EntityScope parentScope;
  private final Optional<TypeReference> superClass;
  private final ImmutableList<TypeReference> interfaces;
  private final Map<String, ClassEntity> innerClasses;
  private final ImmutableList<TypeParameter> typeParameters;

  public ClassEntity(
      String simpleName,
      Entity.Kind kind,
      List<String> qualifiers,
      boolean isStatic,
      EntityScope parentScope,
      Optional<TypeReference> superClass,
      List<TypeReference> interfaces,
      List<TypeParameter> typeParameters,
      Range<Integer> classNameRage) {
    super(simpleName, kind, qualifiers, isStatic, classNameRage);
    checkArgument(
        ALLOWED_KINDS.contains(kind),
        "Invalid entity kind %s, allowed kinds are %s",
        kind,
        ALLOWED_KINDS);
    this.fields = new HashMap<>();
    this.methods = HashMultimap.create();
    this.parentScope = parentScope;
    this.superClass = superClass;
    this.interfaces = ImmutableList.copyOf(interfaces);
    this.typeParameters = ImmutableList.copyOf(typeParameters);
    this.innerClasses = new HashMap<>();
  }

  @Override
  public boolean isInstanceMember() {
    // Inner classes can be in non-instance context of enclosing classes, regardless wether
    // it's static or not.
    return false;
  }

  @Override
  public ClassEntity getChildScope() {
    return this;
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    return builder
        .putAll(fields.entrySet())
        .putAll(methods)
        .putAll(innerClasses.entrySet())
        .build();
  }

  @Override
  public void addEntity(Entity entity) {
    if (entity instanceof ClassEntity) {
      innerClasses.put(entity.getSimpleName(), (ClassEntity) entity);
    } else if (entity instanceof MethodEntity) {
      methods.put(entity.getSimpleName(), (MethodEntity) entity);
    } else {
      fields.put(entity.getSimpleName(), (VariableEntity) entity);
    }
  }

  public List<MethodEntity> getMethodsWithName(String simpleName) {
    return ImmutableList.copyOf(methods.get(simpleName));
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

  public ImmutableList<TypeParameter> getTypeParameters() {
    return typeParameters;
  }

  @Override
  public String toString() {
    return "ClassEntity<"
        + QualifiedNames.formatQualifiedName(getQualifiers(), getSimpleName())
        + "<"
        + typeParameters
        + ">>";
  }
}
