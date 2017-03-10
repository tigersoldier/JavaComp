package org.javacomp.typesolver;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.PackageScope;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;

/** Logic for solving the type of a given entity. */
public class TypeSolver {
  private static final Optional<SolvedType> UNSOLVED = Optional.empty();
  private static final Set<Entity.Kind> CLASS_KINDS = ClassEntity.ALLOWED_KINDS;

  public Optional<SolvedType> solve(
      TypeReference typeReference, GlobalScope globalScope, EntityScope parentScope) {
    if (typeReference.isPrimitive()) {
      return Optional.of(
          createSolvedType(PrimitiveEntity.get(typeReference.getSimpleName()), typeReference));
    }

    List<String> fullName = typeReference.getFullName();
    ClassEntity currentClass =
        (ClassEntity) findEntityInScope(fullName.get(0), globalScope, parentScope, CLASS_KINDS);
    // Find the rest of the name parts, if exist.
    for (int i = 1; currentClass != null && i < fullName.size(); i++) {
      String innerClassName = fullName.get(i);
      currentClass =
          (ClassEntity) findClassMember(innerClassName, currentClass, globalScope, CLASS_KINDS);
      if (currentClass == null) {
        return Optional.empty();
      }
    }
    if (currentClass != null) {
      return Optional.of(createSolvedType(currentClass, typeReference));
    }

    // The first part of the type full name is not known class inside the package. Try to find in
    // global package.
    ClassEntity classInGlobalScope =
        findClassInGlobalScope(globalScope, typeReference.getFullName());
    if (classInGlobalScope != null) {
      return Optional.of(createSolvedType(classInGlobalScope, typeReference));
    }
    return Optional.empty();
  }

  /**
   * @param baseScope the {@link EntityScope} to start searching from. Must be one of {@link
   *     GlobalScope}, {@link PackageScope}, or {@link ClassEntity}
   * @return {@code null} if not found
   */
  @Nullable
  private EntityScope findClassOrPackage(
      EntityScope baseScope, List<String> qualifiers, GlobalScope globalScope) {
    EntityScope currentScope = baseScope;
    for (String qualifier : qualifiers) {
      if (currentScope instanceof GlobalScope || currentScope instanceof PackageScope) {
        // All members of GlobalScope or PackageScope are either package or class
        Collection<Entity> entities = currentScope.getMemberEntities().get(qualifier);
        if (entities.size() != 1) {
          // Either not found, or is ambiguous.
          return null;
        }
        currentScope = Iterables.getOnlyElement(entities).getChildScope();
      } else if (currentScope instanceof ClassEntity) {
        currentScope =
            findClassMember(qualifier, (ClassEntity) currentScope, globalScope, CLASS_KINDS)
                .getChildScope();
        if (currentScope == null) {
          return null;
        }
      }
    }
    return currentScope;
  }

  @Nullable
  private ClassEntity findClassInGlobalScope(GlobalScope globalScope, List<String> qualifiers) {
    EntityScope classInGlobalScope = findClassOrPackage(globalScope, qualifiers, globalScope);
    if (classInGlobalScope instanceof ClassEntity) {
      return (ClassEntity) classInGlobalScope;
    }
    return null;
  }

  @Nullable
  Entity findEntityInScope(
      String name, GlobalScope globalScope, EntityScope baseScope, Set<Entity.Kind> allowedKinds) {
    return Iterables.getFirst(
        findEntitiesInScope(name, globalScope, baseScope, allowedKinds), null);
  }

  List<Entity> findEntitiesInScope(
      String name, GlobalScope globalScope, EntityScope baseScope, Set<Entity.Kind> allowedKinds) {
    // Search class from the narrowest scope to wider scope.
    List<Entity> foundEntities = ImmutableList.of();
    FileScope fileScope = null;
    for (Optional<EntityScope> currentScope = Optional.of(baseScope);
        currentScope.isPresent();
        currentScope = currentScope.get().getParentScope()) {
      if (currentScope.get() instanceof ClassEntity) {
        ClassEntity classEntity = (ClassEntity) currentScope.get();
        foundEntities = findClassMembers(name, classEntity, globalScope, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
        if (allowedKinds.contains(classEntity.getKind())
            && Objects.equals(name, classEntity.getSimpleName())) {
          return ImmutableList.of(classEntity);
        }
      } else if (currentScope.get() instanceof FileScope) {
        fileScope = (FileScope) currentScope.get();
        foundEntities = findEntitiesInFile(name, fileScope, globalScope, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
      }
      // TODO: handle annonymous class
    }

    // Not found in current file. Try to find in the same package.
    if (fileScope != null) {
      List<String> packageQualifiers = fileScope.getPackageQualifiers();
      PackageScope packageScope = findPackage(globalScope, packageQualifiers);
      if (packageScope != null) {
        Entity foundEntity = findClassInPackage(name, packageScope);
        if (foundEntity != null) {
          return ImmutableList.of(foundEntity);
        }
      }
    }
    return foundEntities;
  }

  @Nullable
  Entity findEntityMember(
      String name, Entity entity, GlobalScope globalScope, Set<Entity.Kind> allowedKinds) {
    if (entity instanceof ClassEntity) {
      return findClassMember(name, (ClassEntity) entity, globalScope, allowedKinds);
    } else {
      return findDirectMember(name, entity.getChildScope(), allowedKinds);
    }
  }

  @Nullable
  Entity findClassMember(
      String name,
      ClassEntity classEntity,
      GlobalScope globalScope,
      Set<Entity.Kind> allowedKinds) {
    for (ClassEntity classInHierarchy : classHierarchy(classEntity, globalScope)) {
      Entity memberEntity = findDirectMember(name, classInHierarchy, allowedKinds);
      if (memberEntity != null) {
        return memberEntity;
      }
    }
    return null;
  }

  List<Entity> findClassMembers(
      String name,
      ClassEntity classEntity,
      GlobalScope globalScope,
      Set<Entity.Kind> allowedKinds) {
    // Non-method members can have only one entity.
    if (!allowedKinds.contains(Entity.Kind.METHOD)) {
      Entity classMember = findClassMember(name, classEntity, globalScope, allowedKinds);
      if (classMember != null) {
        return ImmutableList.of(classMember);
      } else {
        return ImmutableList.of();
      }
    }

    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    if (allowedKinds.size() > 1) {
      // Contains non-method members, don't look for all of them, just get the applicable one.
      Set<Entity.Kind> nonMethodKinds =
          Sets.filter(allowedKinds, kind -> kind != Entity.Kind.METHOD);
      Entity nonMemberEntity = findClassMember(name, classEntity, globalScope, nonMethodKinds);
      if (nonMemberEntity != null) {
        builder.add(nonMemberEntity);
      }
    }

    for (ClassEntity classInHierarchy : classHierarchy(classEntity, globalScope)) {
      builder.addAll(classInHierarchy.getMethodsWithName(name));
    }

    return builder.build();
  }

  List<Entity> findClassMethods(String name, ClassEntity classEntity, GlobalScope globalScope) {
    return findClassMembers(
        name, classEntity, globalScope, Sets.immutableEnumSet(Entity.Kind.METHOD));
  }

  @Nullable
  Entity findDirectMember(String name, EntityScope entityScope, Set<Entity.Kind> allowedKinds) {
    for (Entity member : entityScope.getMemberEntities().get(name)) {
      if (allowedKinds.contains(member.getKind())) {
        return member;
      }
    }
    return null;
  }

  private List<Entity> findEntitiesInFile(
      String name, FileScope fileScope, GlobalScope globalScope, Set<Entity.Kind> allowedKinds) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    if (!Sets.intersection(allowedKinds, ClassEntity.ALLOWED_KINDS).isEmpty()) {
      Entity foundClass = findClassInFile(name, fileScope, globalScope);
      if (foundClass != null) {
        builder.add(foundClass);
      }
    }

    if (allowedKinds.contains(Entity.Kind.METHOD)) {
      builder.addAll(findImportedMethodsInFile(name, fileScope, globalScope));
    }

    if (allowedKinds.contains(Entity.Kind.VARIABLE)) {
      Entity foundField = findImportedFieldInFile(name, fileScope, globalScope);
      if (foundField != null) {
        builder.add(foundField);
      }
    }
    return builder.build();
  }

  @Nullable
  private ClassEntity findClassInFile(String name, FileScope fileScope, GlobalScope globalScope) {
    Collection<Entity> entities = fileScope.getMemberEntities().get(name);
    for (Entity entity : entities) {
      if (entity instanceof ClassEntity) {
        return (ClassEntity) entity;
      }
    }
    // Not declared in the file, try imported classes.
    Optional<List<String>> importedClass = fileScope.getImportedClass(name);
    if (importedClass.isPresent()) {
      ClassEntity classInGlobalScope = findClassInGlobalScope(globalScope, importedClass.get());
      if (classInGlobalScope != null) {
        return classInGlobalScope;
      }
    }
    // Not directly imported, try on-demand imports (e.g. import foo.bar.*).
    for (List<String> onDemandClassQualifiers : fileScope.getOnDemandClassImportQualifiers()) {
      EntityScope classOrPackage =
          findClassOrPackage(globalScope /* baseScope */, onDemandClassQualifiers, globalScope);
      if (classOrPackage != null) {
        ClassEntity classEntity = findClass(name, classOrPackage, globalScope);
        if (classEntity != null) {
          return classEntity;
        }
      }
    }

    return null;
  }

  private List<MethodEntity> findImportedMethodsInFile(
      String name, FileScope fileScope, GlobalScope globalScope) {
    // TODO: handle static import.
    return ImmutableList.of();
  }

  @Nullable
  private VariableEntity findImportedFieldInFile(
      String name, FileScope fileScope, GlobalScope globalScope) {
    // TODO: handle static import.
    return null;
  }

  /**
   * Finds a class with given {@code name} in the {@code baseScope}.
   *
   * @param baseScope where to find the class. Must be either a {@link PackageScope} or a {@link
   *     ClassEntity}
   */
  @Nullable
  private ClassEntity findClass(String name, EntityScope baseScope, GlobalScope globalScope) {
    if (baseScope instanceof PackageScope) {
      return findClassInPackage(name, (PackageScope) baseScope);
    } else if (baseScope instanceof ClassEntity) {
      return (ClassEntity) findClassMember(name, (ClassEntity) baseScope, globalScope, CLASS_KINDS);
    }
    return null;
  }

  @Nullable
  private ClassEntity findClassInPackage(String name, PackageScope packageScope) {
    for (Entity entity : packageScope.getMemberEntities().get(name)) {
      if (entity instanceof ClassEntity) {
        return (ClassEntity) entity;
      }
    }
    return null;
  }

  @Nullable
  private PackageScope findPackage(GlobalScope globalScope, List<String> packageQualifiers) {
    PackageScope currentScope = globalScope.getRootPackage();
    for (String qualifier : packageQualifiers) {
      PackageScope nextScope = null;
      for (Entity entity : currentScope.getMemberEntities().get(qualifier)) {
        if (entity instanceof PackageEntity) {
          nextScope = (PackageScope) entity.getChildScope();
          break;
        }
      }
      if (nextScope == null) {
        return null;
      }
      currentScope = nextScope;
    }
    return currentScope;
  }

  private SolvedType createSolvedType(Entity solvedEntity, TypeReference typeReference) {
    return SolvedType.builder()
        .setEntity(solvedEntity)
        .setPrimitive(typeReference.isPrimitive())
        .setArray(typeReference.isArray())
        .build();
  }

  /** Returns an iterable over a class and all its ancestor classes and interfaces. */
  public Iterable<ClassEntity> classHierarchy(ClassEntity classEntity, GlobalScope globalScope) {
    return new Iterable<ClassEntity>() {
      @Override
      public Iterator<ClassEntity> iterator() {
        return new ClassHierarchyIterator(classEntity, globalScope);
      }
    };
  }

  /** An iterator walking through a class and all its ancestor classes and interfaces */
  public class ClassHierarchyIterator extends AbstractIterator<ClassEntity> {
    private class ClassReference {
      private final TypeReference classType;
      private final EntityScope baseScope;

      private ClassReference(TypeReference classType, EntityScope baseScope) {
        this.classType = classType;
        this.baseScope = baseScope;
      }
    }

    private final Deque<ClassReference> classQueue;
    private final ClassEntity classEntity;
    private final GlobalScope globalScope;

    private boolean firstItem;

    public ClassHierarchyIterator(ClassEntity classEntity, GlobalScope globalScope) {
      this.classEntity = classEntity;
      this.globalScope = globalScope;
      this.classQueue = new ArrayDeque<>();
      this.firstItem = true;
    }

    @Override
    protected ClassEntity computeNext() {
      if (firstItem) {
        enqueueSuperClassAndInterfaces(classEntity);
        firstItem = false;
        return classEntity;
      }
      while (!classQueue.isEmpty()) {
        ClassReference classReference = classQueue.removeFirst();
        Optional<SolvedType> solvedType =
            solve(classReference.classType, globalScope, classReference.baseScope);
        if (solvedType.isPresent()) {
          if (solvedType.get().isPrimitive()) {
            throw new RuntimeException(classReference.classType + " " + solvedType);
          }
          enqueueSuperClassAndInterfaces((ClassEntity) solvedType.get().getEntity());
          return (ClassEntity) solvedType.get().getEntity();
        }
      }
      return endOfData();
    }

    private void enqueueSuperClassAndInterfaces(ClassEntity classEntity) {
      if (classEntity.getSuperClass().isPresent() && classEntity.getParentScope().isPresent()) {
        classQueue.addLast(
            new ClassReference(
                classEntity.getSuperClass().get(), classEntity.getParentScope().get()));
      }
      for (TypeReference iface : classEntity.getInterfaces()) {
        classQueue.addLast(new ClassReference(iface, classEntity.getParentScope().get()));
      }
    }
  }
}
