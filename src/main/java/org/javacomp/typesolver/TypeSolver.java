package org.javacomp.typesolver;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.PackageScope;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.TypeReference;

/** Logic for solving the type of a given entity. */
public class TypeSolver {
  private static final Optional<SolvedType> UNSOLVED = Optional.absent();

  public Optional<SolvedType> solve(
      TypeReference typeReference, GlobalScope globalScope, EntityScope parentScope) {
    List<String> fullName = typeReference.getFullName();
    ClassEntity currentClass = findVisibleClass(fullName.get(0), globalScope, parentScope);
    if (currentClass == null) {
      return Optional.absent();
    }
    // Find the rest of the name parts, if exist.
    for (int i = 1; currentClass != null && i < fullName.size(); i++) {
      String innerClassName = fullName.get(i);
      currentClass = findInnerClass(innerClassName, currentClass, globalScope);
      if (currentClass == null) {
        return Optional.absent();
      }
    }
    if (currentClass != null) {
      return Optional.of(new SolvedType(currentClass));
    }

    // The first part of the type full name is not known class inside the package. Try to find in global package.
    ClassEntity classInGlobalScope =
        findClassInGlobalScope(globalScope, typeReference.getFullName());
    if (classInGlobalScope != null) {
      return Optional.of(new SolvedType(classInGlobalScope));
    }
    return Optional.absent();
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
        currentScope = findInnerClass(qualifier, (ClassEntity) currentScope, globalScope);
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
  private ClassEntity findClassInGlobalScope(GlobalScope globalScope, ClassEntity classEntity) {
    List<String> fullName = new ArrayList<>();
    fullName.addAll(classEntity.getQualifiers());
    fullName.add(classEntity.getSimpleName());
    return findClassInGlobalScope(globalScope, fullName);
  }

  @Nullable
  private Optional<FileScope> findFileInGlobalScope(GlobalScope globalScope, FileScope fileScope) {
    return globalScope.getFileScope(fileScope.getFilename());
  }

  @Nullable
  private ClassEntity findVisibleClass(
      String name, GlobalScope globalScope, EntityScope parentScope) {
    // Search class from the narrowest scope to wider scope.
    FileScope fileScope = null;
    ClassEntity foundClass = null;
    for (Optional<EntityScope> currentScope = Optional.of(parentScope);
        currentScope.isPresent();
        currentScope = currentScope.get().getParentScope()) {
      if (currentScope.get() instanceof ClassEntity) {
        ClassEntity classEntity = (ClassEntity) currentScope.get();
        foundClass = findInnerClass(name, classEntity, globalScope);
        if (foundClass != null) {
          return foundClass;
        }
        ClassEntity classInGlobalScope = findClassInGlobalScope(globalScope, classEntity);
        if (classInGlobalScope != null && classInGlobalScope != classEntity) {
          foundClass = findInnerClass(name, classInGlobalScope, globalScope);
          if (foundClass != null) {
            return foundClass;
          }
        }
        if (Objects.equals(name, classEntity.getSimpleName())) {
          return classEntity;
        }
      } else if (currentScope.get() instanceof FileScope) {
        fileScope = (FileScope) currentScope.get();
        foundClass = findClassInFile(name, fileScope, globalScope);
        if (foundClass != null) {
          return foundClass;
        }
        Optional<FileScope> fileInGlobalScope = findFileInGlobalScope(globalScope, fileScope);
        if (fileInGlobalScope.isPresent() && fileInGlobalScope.get() != fileScope) {
          foundClass = findClassInFile(name, fileInGlobalScope.get(), globalScope);
        }
        if (foundClass != null) {
          return foundClass;
        }
      }
      // TODO: handle annonymous class
    }

    // Not found in current file. Try to find in the same package.
    if (fileScope != null) {
      List<String> packageQualifiers = fileScope.getPackageQualifiers();
      PackageScope packageScope = findPackage(globalScope, packageQualifiers);
      if (packageScope != null) {
        foundClass = findClassInPackage(name, packageScope);
        if (foundClass != null) {
          return foundClass;
        }
      }
    }
    return null;
  }

  @Nullable
  private ClassEntity findInnerClass(
      String name, ClassEntity classEntity, GlobalScope globalScope) {
    Map<String, ClassEntity> innerClasses = classEntity.getInnerClasses();
    if (innerClasses.containsKey(name)) {
      return innerClasses.get(name);
    }
    if (classEntity.getSuperClass().isPresent() && classEntity.getParentScope().isPresent()) {
      ClassEntity classInSuperClass =
          findInnerClass(
              name,
              classEntity.getSuperClass().get(),
              globalScope,
              classEntity.getParentScope().get());
      if (classInSuperClass != null) {
        return classInSuperClass;
      }
    }
    for (TypeReference iface : classEntity.getInterfaces()) {
      ClassEntity classInInterface =
          findInnerClass(name, iface, globalScope, classEntity.getParentScope().get());
      if (classInInterface != null) {
        return classInInterface;
      }
    }
    return null;
  }

  @Nullable
  private ClassEntity findInnerClass(
      String name, TypeReference typeReference, GlobalScope globalScope, EntityScope parentScope) {
    Optional<SolvedType> solvedType = solve(typeReference, globalScope, parentScope);
    if (!solvedType.isPresent()) {
      return null;
    }
    return findInnerClass(name, solvedType.get().getClassEntity(), globalScope);
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
      return findInnerClass(name, (ClassEntity) baseScope, globalScope);
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
}
