package org.javacomp.typesolver;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.AggregatePackageScope;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.PackageScope;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;

/** Logic for solving the type of a given entity. */
public class TypeSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Optional<SolvedType> UNSOLVED = Optional.empty();
  private static final Set<Entity.Kind> CLASS_KINDS = ClassEntity.ALLOWED_KINDS;

  public Optional<SolvedType> solve(
      TypeReference typeReference, Module module, EntityScope parentScope) {
    if (typeReference.isPrimitive()) {
      return Optional.of(
          createSolvedType(PrimitiveEntity.get(typeReference.getSimpleName()), typeReference));
    }

    List<String> fullName = typeReference.getFullName();
    Optional<Entity> currentClass =
        findEntityInScope(
            fullName.get(0),
            module,
            parentScope,
            -1 /* position not useful for solving types */,
            CLASS_KINDS);
    // Find the rest of the name parts, if exist.
    for (int i = 1; currentClass.isPresent() && i < fullName.size(); i++) {
      String innerClassName = fullName.get(i);
      currentClass =
          findClassMember(innerClassName, (ClassEntity) currentClass.get(), module, CLASS_KINDS);
      if (!currentClass.isPresent()) {
        return Optional.empty();
      }
    }
    if (currentClass.isPresent()) {
      return Optional.of(createSolvedType(currentClass.get(), typeReference));
    }

    // The first part of the type full name is not known class inside the package. Try to find in
    // global package.
    Optional<Entity> classInModule = findClassInModule(module, typeReference.getFullName());
    if (classInModule.isPresent()) {
      return Optional.of(createSolvedType(classInModule.get(), typeReference));
    }
    return Optional.empty();
  }

  public Optional<Entity> findClassOrPackage(List<String> qualifiers, Module module) {
    EntityScope currentScope = getAggregateRootPackageScope(module);
    Entity currentEntity = null;
    for (String qualifier : qualifiers) {
      if (currentScope instanceof PackageScope) {
        // All members of Module or PackageScope are either package or class
        Collection<Entity> entities = currentScope.getMemberEntities().get(qualifier);
        if (entities.isEmpty()) {
          // Either not found, or is ambiguous.
          currentEntity = null;
          break;
        } else if (entities.size() > 1) {
          logger.warning("More than one class %s are found in package: %s", qualifier, entities);
        }

        currentEntity = Iterables.getFirst(entities, null);
      } else if (currentScope instanceof ClassEntity) {
        currentEntity =
            findClassMember(qualifier, (ClassEntity) currentScope, module, CLASS_KINDS)
                .orElse(null);
        if (currentEntity == null) {
          break;
        }
      }

      if (currentEntity == null) {
        break;
      }

      currentScope = currentEntity.getChildScope();
    }
    if (currentEntity != null) {
      return Optional.of(currentEntity);
    }

    return Optional.empty();
  }

  public AggregatePackageScope getAggregateRootPackageScope(Module module) {
    AggregatePackageScope aggregatedPackageScope = new AggregatePackageScope();
    fillAggregateRootPackageScope(aggregatedPackageScope, module, new HashSet<Module>());
    return aggregatedPackageScope;
  }

  private void fillAggregateRootPackageScope(
      AggregatePackageScope aggregatePackageScope, Module module, Set<Module> visitedModules) {
    if (visitedModules.contains(module)) {
      return;
    }
    visitedModules.add(module);
    aggregatePackageScope.addPackageScope(module.getRootPackage());

    for (Module dependingModule : module.getDependingModules()) {
      fillAggregateRootPackageScope(aggregatePackageScope, dependingModule, visitedModules);
    }
  }

  private Optional<Entity> findClassInModule(Module module, List<String> qualifiers) {
    Optional<Entity> classInModule = findClassOrPackage(qualifiers, module);
    if (classInModule.isPresent() && classInModule.get() instanceof ClassEntity) {
      return classInModule;
    }

    return Optional.empty();
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  Optional<Entity> findEntityInScope(
      String name,
      Module module,
      EntityScope baseScope,
      int position,
      Set<Entity.Kind> allowedKinds) {
    List<Entity> entities = findEntitiesInScope(name, module, baseScope, position, allowedKinds);
    if (entities.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(entities.get(0));
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  public List<Entity> findEntitiesInScope(
      String name,
      Module module,
      EntityScope baseScope,
      int position,
      Set<Entity.Kind> allowedKinds) {
    // Search class from the narrowest scope to wider scope.
    List<Entity> foundEntities = ImmutableList.of();
    FileScope fileScope = null;
    for (Optional<EntityScope> currentScope = Optional.of(baseScope);
        currentScope.isPresent();
        currentScope = currentScope.get().getParentScope()) {
      if (currentScope.get() instanceof ClassEntity) {
        ClassEntity classEntity = (ClassEntity) currentScope.get();
        foundEntities = findClassMembers(name, classEntity, module, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
        if (allowedKinds.contains(classEntity.getKind())
            && Objects.equals(name, classEntity.getSimpleName())) {
          return ImmutableList.of(classEntity);
        }
      } else if (currentScope.get() instanceof FileScope) {
        fileScope = (FileScope) currentScope.get();
        foundEntities = findEntitiesInFile(name, fileScope, module, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
      } else {
        // Block-like scopes (method, if, for, etc...)
        foundEntities =
            findEntitiesInBlock(name, currentScope.get(), module, position, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
      }
      // TODO: handle annonymous class
    }

    // Not found in current file. Try to find in the same package.
    if (fileScope != null) {
      List<String> packageQualifiers = fileScope.getPackageQualifiers();
      Optional<PackageScope> packageScope = findPackage(module, packageQualifiers);
      if (packageScope.isPresent()) {
        Optional<Entity> foundEntity = findClassInPackage(name, packageScope.get());
        if (foundEntity.isPresent()) {
          return ImmutableList.of(foundEntity.get());
        }
      }
    }
    return foundEntities;
  }

  Optional<Entity> findEntityMember(
      String name, Entity entity, Module module, Set<Entity.Kind> allowedKinds) {
    if (entity instanceof ClassEntity) {
      return findClassMember(name, (ClassEntity) entity, module, allowedKinds);
    } else {
      return findDirectMember(name, entity.getChildScope(), allowedKinds);
    }
  }

  Optional<Entity> findClassMember(
      String name, ClassEntity classEntity, Module module, Set<Entity.Kind> allowedKinds) {
    for (ClassEntity classInHierarchy : classHierarchy(classEntity, module)) {
      Optional<Entity> memberEntity = findDirectMember(name, classInHierarchy, allowedKinds);
      if (memberEntity.isPresent()) {
        return memberEntity;
      }
    }
    return Optional.empty();
  }

  List<Entity> findClassMembers(
      String name, ClassEntity classEntity, Module module, Set<Entity.Kind> allowedKinds) {
    // Non-method members can have only one entity.
    if (!allowedKinds.contains(Entity.Kind.METHOD)) {
      Optional<Entity> classMember = findClassMember(name, classEntity, module, allowedKinds);
      if (classMember.isPresent()) {
        return ImmutableList.of(classMember.get());
      } else {
        return ImmutableList.of();
      }
    }

    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    if (allowedKinds.size() > 1) {
      // Contains non-method members, don't look for all of them, just get the applicable one.
      Set<Entity.Kind> nonMethodKinds =
          Sets.filter(allowedKinds, kind -> kind != Entity.Kind.METHOD);
      Optional<Entity> nonMemberEntity = findClassMember(name, classEntity, module, nonMethodKinds);
      if (nonMemberEntity.isPresent()) {
        builder.add(nonMemberEntity.get());
      }
    }

    for (ClassEntity classInHierarchy : classHierarchy(classEntity, module)) {
      builder.addAll(classInHierarchy.getMethodsWithName(name));
    }

    return builder.build();
  }

  List<Entity> findClassMethods(String name, ClassEntity classEntity, Module module) {
    return findClassMembers(name, classEntity, module, Sets.immutableEnumSet(Entity.Kind.METHOD));
  }

  Optional<Entity> findDirectMember(
      String name, EntityScope entityScope, Set<Entity.Kind> allowedKinds) {
    for (Entity member : entityScope.getMemberEntities().get(name)) {
      if (allowedKinds.contains(member.getKind())) {
        return Optional.of(member);
      }
    }
    return Optional.empty();
  }

  private List<Entity> findEntitiesInFile(
      String name, FileScope fileScope, Module module, Set<Entity.Kind> allowedKinds) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    if (!Sets.intersection(allowedKinds, ClassEntity.ALLOWED_KINDS).isEmpty()) {
      Optional<Entity> foundClass = findClassInFile(name, fileScope, module);
      if (foundClass.isPresent()) {
        builder.add(foundClass.get());
      }
    }

    if (allowedKinds.contains(Entity.Kind.METHOD)) {
      builder.addAll(findImportedMethodsInFile(name, fileScope, module));
    }

    if (allowedKinds.contains(Entity.Kind.FIELD)) {
      Optional<VariableEntity> foundField = findImportedFieldInFile(name, fileScope, module);
      if (foundField.isPresent()) {
        builder.add(foundField.get());
      }
    }
    return builder.build();
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  private List<Entity> findEntitiesInBlock(
      String name,
      EntityScope baseScope,
      Module module,
      int position,
      Set<Entity.Kind> allowedKinds) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    if (allowedKinds.contains(Entity.Kind.VARIABLE)) {
      allowedKinds = Sets.difference(allowedKinds, EnumSet.of(Entity.Kind.VARIABLE));

      while (baseScope != null && !(baseScope instanceof ClassEntity)) {
        baseScope
            .getMemberEntities()
            .get(name)
            .stream()
            .filter(
                entity -> {
                  if (position >= 0
                      && entity.getKind() == Entity.Kind.VARIABLE
                      && entity.getSymbolRange().lowerEndpoint() > position) {
                    // Filter out variables defined after position.
                    return false;
                  }
                  return true;
                })
            .forEach(entity -> builder.add(entity));

        baseScope = baseScope.getParentScope().orElse(null);
      }
    }

    if (baseScope instanceof ClassEntity && !allowedKinds.isEmpty()) {
      builder.addAll(findClassMembers(name, (ClassEntity) baseScope, module, allowedKinds));
    }

    return builder.build();
  }

  private Optional<Entity> findClassInFile(String name, FileScope fileScope, Module module) {
    Collection<Entity> entities = fileScope.getMemberEntities().get(name);
    for (Entity entity : entities) {
      if (entity instanceof ClassEntity) {
        return Optional.of(entity);
      }
    }
    // Not declared in the file, try imported classes.
    Optional<List<String>> importedClass = fileScope.getImportedClass(name);
    if (importedClass.isPresent()) {
      Optional<Entity> classInModule = findClassInModule(module, importedClass.get());
      if (classInModule.isPresent()) {
        return classInModule;
      }
    }
    // Not directly imported, try on-demand imports (e.g. import foo.bar.*).
    for (List<String> onDemandClassQualifiers : fileScope.getOnDemandClassImportQualifiers()) {
      Optional<Entity> classOrPackage = findClassOrPackage(onDemandClassQualifiers, module);
      if (classOrPackage.isPresent()) {
        Optional<Entity> classEntity =
            findClass(name, classOrPackage.get().getChildScope(), module);
        if (classEntity.isPresent()) {
          return classEntity;
        }
      }
    }

    return Optional.empty();
  }

  private List<MethodEntity> findImportedMethodsInFile(
      String name, FileScope fileScope, Module module) {
    // TODO: handle static import.
    return ImmutableList.of();
  }

  private Optional<VariableEntity> findImportedFieldInFile(
      String name, FileScope fileScope, Module module) {
    // TODO: handle static import.
    return Optional.empty();
  }

  /**
   * Finds a class with given {@code name} in the {@code baseScope}.
   *
   * @param baseScope where to find the class. Must be either a {@link PackageScope} or a {@link
   *     ClassEntity}
   */
  private Optional<Entity> findClass(String name, EntityScope baseScope, Module module) {
    if (baseScope instanceof PackageScope) {
      return findClassInPackage(name, (PackageScope) baseScope);
    } else if (baseScope instanceof ClassEntity) {
      return findClassMember(name, (ClassEntity) baseScope, module, CLASS_KINDS);
    }
    return Optional.empty();
  }

  private Optional<Entity> findClassInPackage(String name, PackageScope packageScope) {
    for (Entity entity : packageScope.getMemberEntities().get(name)) {
      if (entity instanceof ClassEntity) {
        return Optional.of(entity);
      }
    }
    return Optional.empty();
  }

  private Optional<PackageScope> findPackage(Module module, List<String> packageQualifiers) {
    PackageScope currentScope = getAggregateRootPackageScope(module);
    for (String qualifier : packageQualifiers) {
      PackageScope nextScope = null;
      for (Entity entity : currentScope.getMemberEntities().get(qualifier)) {
        if (entity instanceof PackageEntity) {
          nextScope = (PackageScope) entity.getChildScope();
          break;
        }
      }
      if (nextScope == null) {
        return Optional.empty();
      }
      currentScope = nextScope;
    }
    return Optional.of(currentScope);
  }

  private SolvedType createSolvedType(Entity solvedEntity, TypeReference typeReference) {
    return SolvedType.builder()
        .setEntity(solvedEntity)
        .setPrimitive(typeReference.isPrimitive())
        .setArray(typeReference.isArray())
        .build();
  }

  /** Returns an iterable over a class and all its ancestor classes and interfaces. */
  public Iterable<ClassEntity> classHierarchy(ClassEntity classEntity, Module module) {
    return new Iterable<ClassEntity>() {
      @Override
      public Iterator<ClassEntity> iterator() {
        return new ClassHierarchyIterator(classEntity, module);
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
    private final Module module;

    private boolean firstItem;

    public ClassHierarchyIterator(ClassEntity classEntity, Module module) {
      this.classEntity = classEntity;
      this.module = module;
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
            solve(classReference.classType, module, classReference.baseScope);
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
