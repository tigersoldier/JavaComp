package org.javacomp.typesolver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
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
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.PackageScope;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedArrayType;
import org.javacomp.model.SolvedPackageType;
import org.javacomp.model.SolvedPrimitiveType;
import org.javacomp.model.SolvedReferenceType;
import org.javacomp.model.SolvedType;
import org.javacomp.model.SolvedTypeParameters;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeParameter;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.WildcardTypeArgument;

/** Logic for solvfing the type of a given entity. */
public class TypeSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Optional<SolvedType> UNSOLVED = Optional.empty();
  private static final Set<Entity.Kind> CLASS_KINDS = ClassEntity.ALLOWED_KINDS;
  private static final Set<Entity.Kind> CLASS_OR_PACKAGE_KINDS =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.QUALIFIER)
          .build();
  public static final List<String> JAVA_LANG_QUALIFIERS = ImmutableList.of("java", "lang");
  public static final List<String> JAVA_LANG_OBJECT_QUALIFIERS =
      ImmutableList.of("java", "lang", "Object");
  public static final List<String> JAVA_LANG_STRING_QUALIFIERS =
      ImmutableList.of("java", "lang", "String");

  public Optional<SolvedType> solve(
      TypeReference typeReference, EntityScope parentScope, Module module) {
    return solve(
        typeReference, solveTypeParametersInScope(parentScope, module), parentScope, module);
  }

  public Optional<SolvedType> solve(
      TypeReference typeReference,
      SolvedTypeParameters contextTypeParameters,
      EntityScope parentScope,
      Module module) {
    if (typeReference.isPrimitive()) {
      return Optional.of(
          createSolvedType(
              PrimitiveEntity.get(typeReference.getSimpleName()),
              typeReference,
              contextTypeParameters,
              parentScope,
              module));
    }

    List<String> fullName = typeReference.getFullName();

    if (fullName.isEmpty()) {
      // There can be two casese where the type reference can be empty:
      //   1) The return type of class constructors.
      //   2) The type of implicit lambda function.
      //
      // Returning empty solved type for now.
      //
      // TODO: solve() should never be called for case 1. For case 2 we should infer the type
      // from the context.
      return Optional.empty();
    }

    // Try to lookup in type parameters first.
    if (fullName.size() == 1) {
      Optional<SolvedType> typeInTypeParameters =
          contextTypeParameters.getTypeParameter(typeReference.getSimpleName());
      if (typeInTypeParameters.isPresent()) {
        return typeInTypeParameters;
      }
    }

    Optional<ClassEntity> foundClass = findClassFromClassOrFile(fullName, parentScope, module);
    return foundClass.map(
        classEntity ->
            createSolvedType(
                classEntity, typeReference, contextTypeParameters, parentScope, module));
  }

  public Optional<Entity> findClassOrPackageInModule(List<String> qualifiers, Module module) {
    return findClassOrPackageInModule(qualifiers, module, false /* useCanonicalName */);
  }

  /**
   * @param useCanonicalName if set to true, consider qualifiers the canonical name of the class or
   *     package to look for, otherwise it's the fully qualified name. The differences are
   *     documented by JLS 6.7. In short, fully qualified name allows inner classes declared in
   *     super classes or interfaces, while canonical name only allows inner classes declared in the
   *     parent class itself. See
   *     https://docs.oracle.com/javase/specs/jls/se8/html/jls-6.html#jls-6.7
   */
  private Optional<Entity> findClassOrPackageInModule(
      List<String> qualifiers, Module module, boolean useCanonicalName) {
    EntityScope currentScope = getAggregateRootPackageScope(module);
    if (qualifiers.isEmpty()) {
      return Optional.empty();
    }

    Optional<Entity> currentEntity = Optional.empty();
    for (String qualifier : qualifiers) {
      currentEntity =
          findClassOrPackageInClassOrPackage(qualifier, currentScope, module, useCanonicalName);
      if (!currentEntity.isPresent()) {
        break;
      }
      currentScope = currentEntity.get().getChildScope();
    }
    if (currentEntity.isPresent()) {
      return currentEntity;
    }

    // Try finding in java.lang
    Optional<Entity> classInJavaLang = findClassInPackage(qualifiers, JAVA_LANG_QUALIFIERS, module);
    if (classInJavaLang.isPresent()) {
      return Optional.of(classInJavaLang.get());
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

  private Optional<Entity> findClassInPackage(
      List<String> qualifiedName, List<String> packageQualifiers, Module module) {
    if (qualifiedName.isEmpty()) {
      return Optional.empty();
    }

    Optional<? extends EntityScope> currentScope = findPackageInModule(packageQualifiers, module);
    Optional<Entity> currentEntity = Optional.empty();
    for (String name : qualifiedName) {
      if (!currentScope.isPresent()) {
        return Optional.empty();
      }
      currentEntity =
          findClassOrPackageInClassOrPackage(
              name, currentScope.get(), module, false /* useCanonicalName */);
      currentScope = currentEntity.map(entity -> entity.getChildScope());
    }
    return currentEntity.filter(entity -> entity instanceof ClassEntity);
  }

  public Optional<SolvedType> solveJavaLangObject(Module module) {
    return findClassInModule(JAVA_LANG_OBJECT_QUALIFIERS, module)
        .map(
            entity ->
                createSolvedEntityType(
                    entity,
                    ImmutableList.<TypeArgument>of(),
                    SolvedTypeParameters.EMPTY,
                    entity.getChildScope(),
                    module));
  }

  public Optional<Entity> findClassInModule(List<String> qualifiers, Module module) {
    return findClassInModule(qualifiers, module, false /* useCanonicalName */);
  }

  private Optional<Entity> findClassInModule(
      List<String> qualifiers, Module module, boolean useCanonicalName) {
    Optional<Entity> classInModule =
        findClassOrPackageInModule(qualifiers, module, useCanonicalName);
    return classInModule.filter(entity -> entity instanceof ClassEntity);
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  Optional<EntityWithContext> findEntityFromScope(
      String name,
      EntityScope baseScope,
      Module module,
      int position,
      Set<Entity.Kind> allowedKinds) {
    List<EntityWithContext> entities =
        findEntitiesFromScope(name, baseScope, module, position, allowedKinds);
    if (entities.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(entities.get(0));
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  public List<EntityWithContext> findEntitiesFromScope(
      String name,
      EntityScope baseScope,
      Module module,
      int position,
      Set<Entity.Kind> allowedKinds) {
    // Search class from the narrowest scope to wider scope.
    FileScope fileScope = null;
    SolvedTypeParameters typeParameterInScope = null;
    for (Optional<EntityScope> currentScope = Optional.of(baseScope);
        currentScope.isPresent();
        currentScope = currentScope.get().getParentScope()) {
      if (currentScope.get() instanceof ClassEntity) {
        ClassEntity classEntity = (ClassEntity) currentScope.get();
        EntityWithContext classWithContext = solveClassWithContext(classEntity, module);
        List<EntityWithContext> foundClassMembers =
            findClassMembers(name, classWithContext, module, allowedKinds);
        if (!foundClassMembers.isEmpty()) {
          return foundClassMembers;
        }
        if (allowedKinds.contains(classEntity.getKind())
            && Objects.equals(name, classEntity.getSimpleName())) {
          return ImmutableList.of(classWithContext);
        }
      } else if (currentScope.get() instanceof FileScope) {
        fileScope = (FileScope) currentScope.get();
        List<Entity> foundEntities = findEntitiesInFile(name, fileScope, module, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities
              .stream()
              .map(entity -> EntityWithContext.ofEntity(entity))
              .collect(ImmutableList.toImmutableList());
        }
      } else {
        // Block-like scopes (method, if, for, etc...)
        if (typeParameterInScope == null) {
          typeParameterInScope = solveTypeParametersInScope(currentScope.get(), module);
        }
        List<EntityWithContext> foundEntities =
            findEntitiesInBlock(
                name, typeParameterInScope, currentScope.get(), module, position, allowedKinds);
        if (!foundEntities.isEmpty()) {
          return foundEntities;
        }
      }
      // TODO: handle annonymous class

      // Clear type parameter in scope if we are exiting a scope that defines type parameters,
      // because they are likely to change.
      //
      // TODO: also clear when exiting a static scope.
      if (typeParameterInScope != null && !getTypeParametersOfScope(currentScope.get()).isEmpty()) {
        typeParameterInScope = null;
      }
    }

    // Not found in current file. Try to find in the same package.
    if (fileScope != null) {
      Optional<Entity> classInPackageOfFile = findClassInPackageOfFile(name, fileScope, module);
      if (classInPackageOfFile.isPresent()) {
        return ImmutableList.of(EntityWithContext.ofEntity(classInPackageOfFile.get()));
      }
    }
    return ImmutableList.of();
  }

  Optional<EntityWithContext> findEntityMember(
      String name,
      EntityWithContext entityWithContext,
      Module module,
      Set<Entity.Kind> allowedKinds) {
    if (entityWithContext.getEntity() instanceof ClassEntity) {
      return findClassMember(name, entityWithContext, module, allowedKinds);
    } else {
      return findDirectMember(name, entityWithContext, allowedKinds);
    }
  }

  Optional<EntityWithContext> findClassMember(
      String name,
      EntityWithContext classWithContext,
      Module module,
      Set<Entity.Kind> allowedKinds) {
    checkArgument(
        classWithContext.getEntity() instanceof ClassEntity,
        "Cannot find class member: entity of %s is not a class",
        classWithContext);
    for (EntityWithContext classInHierarchy : classHierarchy(classWithContext, module)) {
      Optional<EntityWithContext> memberEntity =
          findDirectMember(name, classInHierarchy, allowedKinds);
      if (memberEntity.isPresent()) {
        return memberEntity;
      }
    }
    return Optional.empty();
  }

  List<EntityWithContext> findClassMembers(
      String name,
      EntityWithContext classWithContext,
      Module module,
      Set<Entity.Kind> allowedKinds) {
    // Non-method members can have only one entity.
    if (!allowedKinds.contains(Entity.Kind.METHOD)) {
      Optional<EntityWithContext> classMember =
          findClassMember(name, classWithContext, module, allowedKinds);
      if (classMember.isPresent()) {
        return ImmutableList.of(classMember.get());
      } else {
        return ImmutableList.of();
      }
    }

    ImmutableList.Builder<EntityWithContext> builder = new ImmutableList.Builder<>();
    if (allowedKinds.size() > 1) {
      // Contains non-method members, don't look for all of them, just get the applicable one.
      Set<Entity.Kind> nonMethodKinds =
          Sets.filter(allowedKinds, kind -> kind != Entity.Kind.METHOD);
      Optional<EntityWithContext> nonMemberEntity =
          findClassMember(name, classWithContext, module, nonMethodKinds);
      if (nonMemberEntity.isPresent()) {
        builder.add(nonMemberEntity.get());
      }
    }

    for (EntityWithContext classInHierarchy : classHierarchy(classWithContext, module)) {
      checkState(
          classInHierarchy.getEntity() instanceof ClassEntity,
          "Class in hierachy %s in %s is not a class entity",
          classInHierarchy,
          classWithContext);
      builder.addAll(
          ((ClassEntity) classInHierarchy.getEntity())
              .getMethodsWithName(name)
              .stream()
              .map(
                  method ->
                      EntityWithContext.simpleBuilder()
                          .setEntity(method)
                          .setSolvedTypeParameters(classInHierarchy.getSolvedTypeParameters())
                          .build())
              .collect(ImmutableList.toImmutableList()));
    }

    return builder.build();
  }

  List<EntityWithContext> findClassMethods(
      String name, EntityWithContext classWithContext, Module module) {
    checkArgument(
        classWithContext.getEntity() instanceof ClassEntity,
        "findClassMethods requires class entity, but got %s",
        classWithContext);
    return findClassMembers(
        name, classWithContext, module, Sets.immutableEnumSet(Entity.Kind.METHOD));
  }

  Optional<EntityWithContext> findDirectMember(
      String name, EntityWithContext entityWithContext, Set<Entity.Kind> allowedKinds) {
    for (Entity member :
        entityWithContext.getEntity().getChildScope().getMemberEntities().get(name)) {
      if (allowedKinds.contains(member.getKind())) {
        return Optional.of(
            EntityWithContext.simpleBuilder()
                .setEntity(member)
                .setSolvedTypeParameters(entityWithContext.getSolvedTypeParameters())
                .build());
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

  private Optional<ClassEntity> findClassFromClassOrFile(
      List<String> fullName, EntityScope classOrPackage, Module module) {
    if (fullName.isEmpty()) {
      return Optional.empty();
    }
    Optional<Entity> foundClassOrPackage =
        findClassOrPackageFromClassOrFile(fullName.get(0), classOrPackage, module);
    for (int i = 1; i < fullName.size() && foundClassOrPackage.isPresent(); i++) {
      foundClassOrPackage =
          findClassOrPackageInClassOrPackage(
              fullName.get(i),
              foundClassOrPackage.get().getChildScope(),
              module,
              false /* useCanonicalName */);
    }

    return foundClassOrPackage
        .filter(entity -> entity instanceof ClassEntity)
        .map(entity -> (ClassEntity) entity);
  }

  private Optional<Entity> findClassOrPackageFromClassOrFile(
      String name, EntityScope classOrPackage, Module module) {
    for (EntityScope currentScope = classOrPackage;
        currentScope != null;
        currentScope = currentScope.getParentScope().orElse(null)) {
      if (currentScope instanceof ClassEntity) {
        Optional<Entity> innerClass =
            findInnerClassInClassHierachy(name, (ClassEntity) currentScope, module);
        if (innerClass.isPresent()) {
          return innerClass;
        }
      } else if (currentScope instanceof FileScope) {
        for (Entity entity :
            findEntitiesInFile(name, (FileScope) currentScope, module, CLASS_OR_PACKAGE_KINDS)) {
          return Optional.of(entity);
        }
        Optional<Entity> classInPackageOfFile =
            findClassInPackageOfFile(name, (FileScope) currentScope, module);
        if (classInPackageOfFile.isPresent()) {
          return classInPackageOfFile;
        }
      }
    }

    // Find in module
    for (Entity entity : getAggregateRootPackageScope(module).getMemberEntities().get(name)) {
      if (entity instanceof ClassEntity || entity instanceof PackageEntity) {
        return Optional.of(entity);
      }
    }

    // Try finding in java.lang
    Optional<Entity> javaLangPackage =
        findClassOrPackageInModule(JAVA_LANG_QUALIFIERS, module, true /* useCanonicalName */);
    if (javaLangPackage.isPresent()) {
      return findClassOrPackageInClassOrPackage(
          name, javaLangPackage.get().getChildScope(), module, true /* useCanonicalName */);
    }

    return Optional.empty();
  }

  private Optional<Entity> findInnerClassInClassHierachy(
      String name, ClassEntity fromClass, Module module) {
    for (ClassEntity classInHierachy : classHierarchyWithoutContext(fromClass, module)) {
      ClassEntity innerClass = classInHierachy.getInnerClasses().get(name);
      if (innerClass != null) {
        return Optional.of(innerClass);
      }
    }
    return Optional.empty();
  }

  private Optional<Entity> findClassOrPackageInClassOrPackage(
      String name, EntityScope classOrPackage, Module module, boolean useCanonicalName) {
    if (classOrPackage instanceof ClassEntity && !useCanonicalName) {
      return findInnerClassInClassHierachy(name, (ClassEntity) classOrPackage, module);
    } else {
      for (Entity entity : classOrPackage.getMemberEntities().get(name)) {
        if (entity instanceof ClassEntity || entity instanceof PackageEntity) {
          return Optional.of(entity);
        }
      }
    }
    return Optional.empty();
  }

  /**
   * @param position the position in the file that the expression is being solved. It's useful for
   *     filtering out variables defined after the position. It's ignored if set to negative value.
   */
  private List<EntityWithContext> findEntitiesInBlock(
      String name,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module,
      int position,
      Set<Entity.Kind> allowedKinds) {
    ImmutableList.Builder<EntityWithContext> builder = new ImmutableList.Builder<>();
    if (allowedKinds.contains(Entity.Kind.VARIABLE)) {
      allowedKinds = Sets.difference(allowedKinds, EnumSet.of(Entity.Kind.VARIABLE));

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
          .forEach(
              entity ->
                  builder.add(
                      EntityWithContext.simpleBuilder()
                          .setEntity(entity)
                          .setSolvedTypeParameters(contextTypeParameters)
                          .build()));

      baseScope = baseScope.getParentScope().orElse(null);
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
      Optional<Entity> classInModule =
          findClassInModule(importedClass.get(), module, true /* useCanonicalName */);
      if (classInModule.isPresent()) {
        return classInModule;
      }
    }
    // Not directly imported, try on-demand imports (e.g. import foo.bar.*).
    for (List<String> onDemandClassQualifiers : fileScope.getOnDemandClassImportQualifiers()) {
      Optional<Entity> classOrPackage =
          findClassOrPackageInModule(onDemandClassQualifiers, module, true /* useCanonicalName */);
      if (classOrPackage.isPresent()) {
        Optional<Entity> foundClassOrPacakge =
            findClassOrPackageInClassOrPackage(
                name, classOrPackage.get().getChildScope(), module, true /* useCanonicalName */);
        if (foundClassOrPacakge.isPresent() && foundClassOrPacakge.get() instanceof ClassEntity) {
          return foundClassOrPacakge;
        }
      }
    }

    return Optional.empty();
  }

  private Optional<Entity> findClassInPackageOfFile(
      String name, FileScope fileScope, Module module) {
    List<String> packageQualifiers = fileScope.getPackageQualifiers();
    Optional<PackageScope> packageScope = findPackageInModule(packageQualifiers, module);
    if (packageScope.isPresent()) {
      return findClassInPackage(name, packageScope.get());
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

  private Optional<Entity> findClassInPackage(String name, PackageScope packageScope) {
    for (Entity entity : packageScope.getMemberEntities().get(name)) {
      if (entity instanceof ClassEntity) {
        return Optional.of(entity);
      }
    }
    return Optional.empty();
  }

  public Optional<PackageScope> findPackageInModule(List<String> packageQualifiers, Module module) {
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

  public SolvedTypeParameters solveTypeParameters(
      List<TypeParameter> typeParameters,
      List<TypeArgument> typeArguments,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module) {
    if (typeParameters.isEmpty()) {
      return SolvedTypeParameters.EMPTY;
    }

    SolvedTypeParameters.Builder builder = SolvedTypeParameters.builder();

    for (int i = 0; i < typeParameters.size(); i++) {
      TypeParameter typeParameter = typeParameters.get(i);
      Optional<SolvedType> solvedTypeParameter;
      if (i < typeArguments.size()) {
        TypeArgument typeArgument = typeArguments.get(i);
        solvedTypeParameter =
            solveTypeArgument(typeArgument, contextTypeParameters, baseScope, module);
      } else {
        // Not enough type arguments. This can be caused by a) using raw type, or b) the code is
        // incorrect. Use the bounds of the type parameters.
        solvedTypeParameter =
            solveTypeParameterBounds(typeParameter, contextTypeParameters, baseScope, module);
      }
      if (solvedTypeParameter.isPresent()) {
        builder.putTypeParameter(typeParameter.getName(), solvedTypeParameter.get());
      }
    }
    return builder.build();
  }

  private Optional<SolvedType> solveTypeArgument(
      TypeArgument typeArgument,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module) {
    if (typeArgument instanceof TypeReference) {
      return solve((TypeReference) typeArgument, contextTypeParameters, baseScope, module);
    } else if (typeArgument instanceof WildcardTypeArgument) {
      WildcardTypeArgument wildCardTypeArgument = (WildcardTypeArgument) typeArgument;
      Optional<WildcardTypeArgument.Bound> bound = wildCardTypeArgument.getBound();
      if (bound.isPresent() && bound.get().getKind() == WildcardTypeArgument.Bound.Kind.EXTENDS) {
        return solve(bound.get().getTypeReference(), contextTypeParameters, baseScope, module);
      } else {
        return solveJavaLangObject(module);
      }
    } else {
      logger.warning("Unsupported type of type argument: %s", typeArgument);
      return Optional.empty();
    }
  }

  private Optional<SolvedType> solveTypeParameterBounds(
      TypeParameter typeParameter,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module) {
    if (contextTypeParameters.getTypeParameter(typeParameter.getName()).isPresent()) {
      return contextTypeParameters.getTypeParameter(typeParameter.getName());
    }
    List<TypeReference> bounds = typeParameter.getExtendBounds();
    if (bounds.isEmpty()) {
      // No bound defined, Object is the bound.
      return solveJavaLangObject(module);
    } else {
      // TODO: support multiple bounds.
      TypeReference bound = bounds.get(0);
      return solve(bound, contextTypeParameters, baseScope, module);
    }
  }

  /**
   * Solve type parameter bindings based on the type parameters declared in the given scope and its
   * parent scopes.
   */
  public SolvedTypeParameters solveTypeParametersInScope(EntityScope baseScope, Module module) {
    Deque<List<TypeParameter>> typeParametersStack = new ArrayDeque<>();
    Deque<EntityScope> entityScopeStack = new ArrayDeque<>();
    for (EntityScope currentScope = baseScope;
        currentScope != null;
        currentScope = currentScope.getParentScope().orElse(null)) {
      List<TypeParameter> typeParameters = getTypeParametersOfScope(currentScope);
      if (!typeParameters.isEmpty()) {
        typeParametersStack.push(typeParameters);
        entityScopeStack.push(currentScope);
      }
    }

    SolvedTypeParameters solvedTypeParameters = SolvedTypeParameters.EMPTY;
    // Solve type parameters from parent scopes to child scopes. Child scopes may reference type
    // parameters defined by parent scopes.
    while (!typeParametersStack.isEmpty()) {
      SolvedTypeParameters.Builder builder = solvedTypeParameters.toBuilder();
      List<TypeParameter> typeParameters = typeParametersStack.pop();
      EntityScope typeParametersScope = entityScopeStack.pop();
      for (TypeParameter typeParameter : typeParameters) {
        Optional<SolvedType> solvedBounds =
            solveTypeParameterBounds(
                typeParameter, solvedTypeParameters, typeParametersScope, module);
        if (solvedBounds.isPresent()) {
          builder.putTypeParameter(typeParameter.getName(), solvedBounds.get());
        }
      }
      solvedTypeParameters = builder.build();
    }
    return solvedTypeParameters;
  }

  private List<TypeParameter> getTypeParametersOfScope(EntityScope entityScope) {
    if (entityScope instanceof ClassEntity) {
      return ((ClassEntity) entityScope).getTypeParameters();
    }
    if (entityScope instanceof MethodEntity) {
      return ((MethodEntity) entityScope).getTypeParameters();
    }
    return ImmutableList.of();
  }

  private SolvedType createSolvedType(
      Entity solvedEntity,
      TypeReference typeReference,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module) {
    if (typeReference.isArray()) {
      return SolvedArrayType.create(
          createSolvedEntityType(
              solvedEntity,
              typeReference.getTypeArguments(),
              contextTypeParameters,
              baseScope,
              module));
    }
    return createSolvedEntityType(
        solvedEntity, typeReference.getTypeArguments(), contextTypeParameters, baseScope, module);
  }

  private EntityWithContext solveClassWithContext(ClassEntity classEntity, Module module) {
    return EntityWithContext.simpleBuilder()
        .setEntity(classEntity)
        .setSolvedTypeParameters(solveTypeParametersInScope(classEntity, module))
        .build();
  }

  private SolvedType createSolvedEntityType(
      Entity solvedEntity,
      List<TypeArgument> typeArguments,
      SolvedTypeParameters contextTypeParameters,
      EntityScope baseScope,
      Module module) {
    if (solvedEntity instanceof ClassEntity) {
      ClassEntity classEntity = (ClassEntity) solvedEntity;
      return SolvedReferenceType.create(
          classEntity,
          solveTypeParameters(
              classEntity.getTypeParameters(),
              typeArguments,
              contextTypeParameters,
              baseScope,
              module));
    } else if (solvedEntity instanceof PrimitiveEntity) {
      return SolvedPrimitiveType.create((PrimitiveEntity) solvedEntity);
    } else if (solvedEntity instanceof PackageEntity) {
      return SolvedPackageType.create((PackageEntity) solvedEntity);
    } else {
      throw new RuntimeException(
          "Unsupported type of entity for creating solved type: " + solvedEntity);
    }
  }

  /** Returns an iterable over a class and all its ancestor classes and interfaces. */
  public Iterable<EntityWithContext> classHierarchy(
      EntityWithContext classWithContext, Module module) {
    return new Iterable<EntityWithContext>() {
      @Override
      public Iterator<EntityWithContext> iterator() {
        return new ClassHierarchyIterator(classWithContext, module, true /* solveTypeParmeters */);
      }
    };
  }

  public Iterable<ClassEntity> classHierarchyWithoutContext(
      ClassEntity classEnitty, Module module) {
    return new Iterable<ClassEntity>() {
      @Override
      public Iterator<ClassEntity> iterator() {
        return Iterators.transform(
            new ClassHierarchyIterator(
                EntityWithContext.ofEntity(classEnitty), module, false /* solveTypeParmeters */),
            classWithContext -> (ClassEntity) classWithContext.getEntity());
      }
    };
  }

  /** An iterator walking through a class and all its ancestor classes and interfaces */
  public class ClassHierarchyIterator extends AbstractIterator<EntityWithContext> {
    private class ClassReference {
      private final TypeReference classType;
      private final EntityWithContext subclassWithContext;

      private ClassReference(TypeReference classType, EntityWithContext subclassWithContext) {
        this.classType = classType;
        this.subclassWithContext = subclassWithContext;
      }
    }

    private final Deque<ClassReference> classQueue;
    private final Set<Entity> visitedClassEntity;
    private final EntityWithContext classWithContext;
    private final Module module;
    private final boolean solveTypeParameters;

    private boolean firstItem;
    private boolean javaLangObjectAdded;

    public ClassHierarchyIterator(
        EntityWithContext classWithContext, Module module, boolean solveTypeParameters) {
      this.classWithContext = classWithContext;
      this.module = module;
      this.solveTypeParameters = solveTypeParameters;
      this.classQueue = new ArrayDeque<>();
      this.visitedClassEntity = new HashSet<>();
      this.firstItem = true;
    }

    @Override
    protected EntityWithContext computeNext() {
      if (firstItem) {
        firstItem = false;
        visitClassAndEnqueueSupers(classWithContext);
        return classWithContext;
      }

      while (!classQueue.isEmpty()) {
        ClassReference classReference = classQueue.removeFirst();
        Optional<EntityWithContext> solvedEntity;
        if (classReference.subclassWithContext == null) {
          solvedEntity =
              findClassInModule(classReference.classType.getFullName(), module)
                  .map(entity -> EntityWithContext.simpleBuilder().setEntity(entity).build());
        } else if (solveTypeParameters) {
          solvedEntity =
              solve(
                      classReference.classType,
                      classReference.subclassWithContext.getSolvedTypeParameters(),
                      classReference
                          .subclassWithContext
                          .getEntity()
                          .getChildScope()
                          .getParentScope()
                          .get(),
                      module)
                  .filter(t -> t instanceof SolvedReferenceType)
                  .map(t -> EntityWithContext.from(t).build());
        } else {
          solvedEntity =
              findClassFromClassOrFile(
                      classReference.classType.getFullName(),
                      classReference
                          .subclassWithContext
                          .getEntity()
                          .getChildScope()
                          .getParentScope()
                          .get(),
                      module)
                  .map(entity -> EntityWithContext.ofEntity(entity));
        }
        if (!solvedEntity.isPresent()) {
          continue;
        }
        EntityWithContext entityWithContext = solvedEntity.get();
        if (!(entityWithContext.getEntity() instanceof ClassEntity)) {
          logger.warning(
              "%s is not a class entity for super class/interface type %s of class %s",
              entityWithContext.getEntity(),
              classReference.classType,
              classReference.subclassWithContext.getEntity());
          continue;
        }

        if (visitedClassEntity.contains(entityWithContext.getEntity())) {
          continue;
        }

        visitClassAndEnqueueSupers(entityWithContext);
        return entityWithContext;
      }

      if (!javaLangObjectAdded) {
        javaLangObjectAdded = true;
        Optional<Entity> javaLangObject = findClassInModule(JAVA_LANG_OBJECT_QUALIFIERS, module);
        if (javaLangObject.isPresent()) {
          return EntityWithContext.simpleBuilder().setEntity(javaLangObject.get()).build();
        }
      }
      return endOfData();
    }

    private void visitClassAndEnqueueSupers(EntityWithContext entityWithContext) {
      visitedClassEntity.add(entityWithContext.getEntity());
      if ("java.lang.Object".equals(entityWithContext.getEntity().getQualifiedName())) {
        javaLangObjectAdded = true;
      }
      enqueueSuperClassAndInterfaces(entityWithContext);
    }

    private void enqueueSuperClassAndInterfaces(EntityWithContext entityWithContext) {
      ClassEntity classEntity = (ClassEntity) entityWithContext.getEntity();
      if (classEntity.getSuperClass().isPresent() && classEntity.getParentScope().isPresent()) {
        classQueue.addLast(
            new ClassReference(classEntity.getSuperClass().get(), entityWithContext));
      } else if (classEntity.getKind() == Entity.Kind.ENUM) {
        classQueue.addLast(new ClassReference(TypeReference.JAVA_LANG_ENUM, null));
      }
      for (TypeReference iface : classEntity.getInterfaces()) {
        classQueue.addLast(new ClassReference(iface, entityWithContext));
      }
    }
  }
}
