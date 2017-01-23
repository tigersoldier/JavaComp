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
import org.javacomp.model.FileIndex;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.PackageIndex;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityIndex;
import org.javacomp.model.TypeReference;

/** Logic for solving the type of a given entity. */
public class TypeSolver {
  private static final Optional<SolvedType> UNSOLVED = Optional.absent();

  public Optional<SolvedType> solve(
      TypeReference typeReference, GlobalIndex globalIndex, EntityIndex parentIndex) {
    List<String> fullName = typeReference.getFullName();
    ClassEntity currentClass = findVisibleClass(fullName.get(0), globalIndex, parentIndex);
    if (currentClass == null) {
      return Optional.absent();
    }
    // Find the rest of the name parts, if exist.
    for (int i = 1; currentClass != null && i < fullName.size(); i++) {
      String innerClassName = fullName.get(i);
      currentClass = findInnerClass(innerClassName, currentClass, globalIndex);
      if (currentClass == null) {
        return Optional.absent();
      }
    }
    if (currentClass != null) {
      return Optional.of(new SolvedType(currentClass));
    }

    // The first part of the type full name is not known class inside the package. Try to find in global package.
    ClassEntity classInGlobalIndex =
        findClassInGlobalIndex(globalIndex, typeReference.getFullName());
    if (classInGlobalIndex != null) {
      return Optional.of(new SolvedType(classInGlobalIndex));
    }
    return Optional.absent();
  }

  /**
   * @param baseIndex the {@link EntityIndex} to start searching from. Must be one of {@link
   *     GlobalIndex}, {@link PackageIndex}, or {@link ClassEntity}
   * @return {@code null} if not found
   */
  @Nullable
  private EntityIndex findClassOrPackage(
      EntityIndex baseIndex, List<String> qualifiers, GlobalIndex globalIndex) {
    EntityIndex currentIndex = baseIndex;
    for (String qualifier : qualifiers) {
      if (currentIndex instanceof GlobalIndex || currentIndex instanceof PackageIndex) {
        // All members of GlobalIndex or PackageIndex are either package or class
        Collection<Entity> entities = currentIndex.getMemberEntities().get(qualifier);
        if (entities.size() != 1) {
          // Either not found, or is ambiguous.
          return null;
        }
        currentIndex = Iterables.getOnlyElement(entities).getChildIndex();
      } else if (currentIndex instanceof ClassEntity) {
        currentIndex = findInnerClass(qualifier, (ClassEntity) currentIndex, globalIndex);
        if (currentIndex == null) {
          return null;
        }
      }
    }
    return currentIndex;
  }

  @Nullable
  private ClassEntity findClassInGlobalIndex(GlobalIndex globalIndex, List<String> qualifiers) {
    EntityIndex classInGlobalIndex = findClassOrPackage(globalIndex, qualifiers, globalIndex);
    if (classInGlobalIndex instanceof ClassEntity) {
      return (ClassEntity) classInGlobalIndex;
    }
    return null;
  }

  @Nullable
  private ClassEntity findClassInGlobalIndex(GlobalIndex globalIndex, ClassEntity classEntity) {
    List<String> fullName = new ArrayList<>();
    fullName.addAll(classEntity.getQualifiers());
    fullName.add(classEntity.getSimpleName());
    return findClassInGlobalIndex(globalIndex, fullName);
  }

  @Nullable
  private Optional<FileIndex> findFileInGlobalIndex(GlobalIndex globalIndex, FileIndex fileIndex) {
    return globalIndex.getFileIndex(fileIndex.getFilename());
  }

  @Nullable
  private ClassEntity findVisibleClass(
      String name, GlobalIndex globalIndex, EntityIndex parentIndex) {
    // Search class from the narrowest scope to wider scope.
    FileIndex fileIndex = null;
    ClassEntity foundClass = null;
    for (Optional<EntityIndex> currentIndex = Optional.of(parentIndex);
        currentIndex.isPresent();
        currentIndex = currentIndex.get().getParentIndex()) {
      if (currentIndex.get() instanceof ClassEntity) {
        ClassEntity classEntity = (ClassEntity) currentIndex.get();
        foundClass = findInnerClass(name, classEntity, globalIndex);
        if (foundClass != null) {
          return foundClass;
        }
        ClassEntity classInGlobalIndex = findClassInGlobalIndex(globalIndex, classEntity);
        if (classInGlobalIndex != null && classInGlobalIndex != classEntity) {
          foundClass = findInnerClass(name, classInGlobalIndex, globalIndex);
          if (foundClass != null) {
            return foundClass;
          }
        }
        if (Objects.equals(name, classEntity.getSimpleName())) {
          return classEntity;
        }
      } else if (currentIndex.get() instanceof FileIndex) {
        fileIndex = (FileIndex) currentIndex.get();
        foundClass = findClassInFile(name, fileIndex, globalIndex);
        if (foundClass != null) {
          return foundClass;
        }
        Optional<FileIndex> fileInGlobalIndex = findFileInGlobalIndex(globalIndex, fileIndex);
        if (fileInGlobalIndex.isPresent() && fileInGlobalIndex.get() != fileIndex) {
          foundClass = findClassInFile(name, fileInGlobalIndex.get(), globalIndex);
        }
        if (foundClass != null) {
          return foundClass;
        }
      }
      // TODO: handle annonymous class
    }

    // Not found in current file. Try to find in the same package.
    if (fileIndex != null) {
      List<String> packageQualifiers = fileIndex.getPackageQualifiers();
      PackageIndex packageIndex = findPackage(globalIndex, packageQualifiers);
      if (packageIndex != null) {
        foundClass = findClassInPackage(name, packageIndex);
        if (foundClass != null) {
          return foundClass;
        }
      }
    }
    return null;
  }

  @Nullable
  private ClassEntity findInnerClass(
      String name, ClassEntity classEntity, GlobalIndex globalIndex) {
    Map<String, ClassEntity> innerClasses = classEntity.getInnerClasses();
    if (innerClasses.containsKey(name)) {
      return innerClasses.get(name);
    }
    if (classEntity.getSuperClass().isPresent() && classEntity.getParentIndex().isPresent()) {
      ClassEntity classInSuperClass =
          findInnerClass(
              name,
              classEntity.getSuperClass().get(),
              globalIndex,
              classEntity.getParentIndex().get());
      if (classInSuperClass != null) {
        return classInSuperClass;
      }
    }
    for (TypeReference iface : classEntity.getInterfaces()) {
      ClassEntity classInInterface =
          findInnerClass(name, iface, globalIndex, classEntity.getParentIndex().get());
      if (classInInterface != null) {
        return classInInterface;
      }
    }
    return null;
  }

  @Nullable
  private ClassEntity findInnerClass(
      String name, TypeReference typeReference, GlobalIndex globalIndex, EntityIndex parentIndex) {
    Optional<SolvedType> solvedType = solve(typeReference, globalIndex, parentIndex);
    if (!solvedType.isPresent()) {
      return null;
    }
    return findInnerClass(name, solvedType.get().getClassEntity(), globalIndex);
  }

  @Nullable
  private ClassEntity findClassInFile(String name, FileIndex fileIndex, GlobalIndex globalIndex) {
    Collection<Entity> entities = fileIndex.getMemberEntities().get(name);
    for (Entity entity : entities) {
      if (entity instanceof ClassEntity) {
        return (ClassEntity) entity;
      }
    }
    // Not declared in the file, try imported classes.
    Optional<List<String>> importedClass = fileIndex.getImportedClass(name);
    if (importedClass.isPresent()) {
      ClassEntity classInGlobalIndex = findClassInGlobalIndex(globalIndex, importedClass.get());
      if (classInGlobalIndex != null) {
        return classInGlobalIndex;
      }
    }
    // Not directly imported, try on-demand imports (e.g. import foo.bar.*).
    for (List<String> onDemandClassQualifiers : fileIndex.getOnDemandClassImportQualifiers()) {
      EntityIndex classOrPackage =
          findClassOrPackage(globalIndex /* baseIndex */, onDemandClassQualifiers, globalIndex);
      if (classOrPackage != null) {
        ClassEntity classEntity = findClass(name, classOrPackage, globalIndex);
        if (classEntity != null) {
          return classEntity;
        }
      }
    }
    return null;
  }

  /**
   * Finds a class with given {@code name} in the {@code baseIndex}.
   *
   * @param baseIndex where to find the class. Must be either a {@link PackageIndex} or a {@link
   *     ClassEntity}
   */
  @Nullable
  private ClassEntity findClass(String name, EntityIndex baseIndex, GlobalIndex globalIndex) {
    if (baseIndex instanceof PackageIndex) {
      return findClassInPackage(name, (PackageIndex) baseIndex);
    } else if (baseIndex instanceof ClassEntity) {
      return findInnerClass(name, (ClassEntity) baseIndex, globalIndex);
    }
    return null;
  }

  @Nullable
  private ClassEntity findClassInPackage(String name, PackageIndex packageIndex) {
    for (Entity entity : packageIndex.getMemberEntities().get(name)) {
      if (entity instanceof ClassEntity) {
        return (ClassEntity) entity;
      }
    }
    return null;
  }

  @Nullable
  private PackageIndex findPackage(GlobalIndex globalIndex, List<String> packageQualifiers) {
    PackageIndex currentIndex = globalIndex.getRootPackage();
    for (String qualifier : packageQualifiers) {
      PackageIndex nextIndex = null;
      for (Entity entity : currentIndex.getMemberEntities().get(qualifier)) {
        if (entity instanceof PackageEntity) {
          nextIndex = (PackageIndex) entity.getChildIndex();
          break;
        }
      }
      if (nextIndex == null) {
        return null;
      }
      currentIndex = nextIndex;
    }
    return currentIndex;
  }
}
