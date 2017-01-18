package org.javacomp.typesolver;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;
import org.javacomp.model.ClassSymbol;
import org.javacomp.model.FileIndex;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.PackageIndex;
import org.javacomp.model.PackageSymbol;
import org.javacomp.model.SolvedType;
import org.javacomp.model.Symbol;
import org.javacomp.model.SymbolIndex;
import org.javacomp.model.TypeReference;

/** Logic for solving the type of a given entity. */
public class TypeSolver {
  private static final Optional<SolvedType> UNSOLVED = Optional.absent();

  public Optional<SolvedType> solve(
      TypeReference typeReference, GlobalIndex globalIndex, SymbolIndex parentIndex) {
    List<String> fullName = typeReference.getFullName();
    ClassSymbol currentClass = findVisibleClass(fullName.get(0), globalIndex, parentIndex);
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
    ClassSymbol classInGlobalIndex =
        findClassInGlobalIndex(globalIndex, typeReference.getFullName());
    if (classInGlobalIndex != null) {
      return Optional.of(new SolvedType(classInGlobalIndex));
    }
    return Optional.absent();
  }

  @Nullable
  private ClassSymbol findClassInGlobalIndex(GlobalIndex globalIndex, List<String> fullName) {
    SymbolIndex currentIndex = globalIndex;
    for (String qualifier : fullName) {
      if (currentIndex instanceof GlobalIndex || currentIndex instanceof PackageIndex) {
        // All members of GlobalIndex or PackageIndex are either package or class
        Collection<Symbol> symbols = currentIndex.getMemberSymbols().get(qualifier);
        if (symbols.size() != 1) {
          // Either not found, or is ambiguous.
          return null;
        }
        currentIndex = Iterables.getOnlyElement(symbols).getChildIndex();
      } else if (currentIndex instanceof ClassSymbol) {
        currentIndex = findInnerClass(qualifier, (ClassSymbol) currentIndex, globalIndex);
        if (currentIndex == null) {
          return null;
        }
      }
    }
    if (currentIndex instanceof ClassSymbol) {
      return (ClassSymbol) currentIndex;
    }
    return null;
  }

  @Nullable
  private ClassSymbol findClassInGlobalIndex(GlobalIndex globalIndex, ClassSymbol classSymbol) {
    List<String> fullName = new ArrayList<>();
    fullName.addAll(classSymbol.getQualifiers());
    fullName.add(classSymbol.getSimpleName());
    return findClassInGlobalIndex(globalIndex, fullName);
  }

  @Nullable
  private Optional<FileIndex> findFileInGlobalIndex(GlobalIndex globalIndex, FileIndex fileIndex) {
    return globalIndex.getFileIndex(fileIndex.getFilename());
  }

  @Nullable
  private ClassSymbol findVisibleClass(
      String name, GlobalIndex globalIndex, SymbolIndex parentIndex) {
    // Search class from the narrowest scope to wider scope.
    FileIndex fileIndex = null;
    ClassSymbol foundClass = null;
    for (Optional<SymbolIndex> currentIndex = Optional.of(parentIndex);
        currentIndex.isPresent();
        currentIndex = currentIndex.get().getParentIndex()) {
      if (currentIndex.get() instanceof ClassSymbol) {
        ClassSymbol classSymbol = (ClassSymbol) currentIndex.get();
        foundClass = findInnerClass(name, classSymbol, globalIndex);
        if (foundClass != null) {
          return foundClass;
        }
        ClassSymbol classInGlobalIndex = findClassInGlobalIndex(globalIndex, classSymbol);
        if (classInGlobalIndex != null && classInGlobalIndex != classSymbol) {
          foundClass = findInnerClass(name, classInGlobalIndex, globalIndex);
          if (foundClass != null) {
            return foundClass;
          }
        }
        if (Objects.equals(name, classSymbol.getSimpleName())) {
          return classSymbol;
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
  private ClassSymbol findInnerClass(
      String name, ClassSymbol classSymbol, GlobalIndex globalIndex) {
    Map<String, ClassSymbol> innerClasses = classSymbol.getInnerClasses();
    if (innerClasses.containsKey(name)) {
      return innerClasses.get(name);
    }
    if (classSymbol.getSuperClass().isPresent() && classSymbol.getParentIndex().isPresent()) {
      ClassSymbol classInSuperClass =
          findInnerClass(
              name,
              classSymbol.getSuperClass().get(),
              globalIndex,
              classSymbol.getParentIndex().get());
      if (classInSuperClass != null) {
        return classInSuperClass;
      }
    }
    for (TypeReference iface : classSymbol.getInterfaces()) {
      ClassSymbol classInInterface =
          findInnerClass(name, iface, globalIndex, classSymbol.getParentIndex().get());
      if (classInInterface != null) {
        return classInInterface;
      }
    }
    return null;
  }

  @Nullable
  private ClassSymbol findInnerClass(
      String name, TypeReference typeReference, GlobalIndex globalIndex, SymbolIndex parentIndex) {
    Optional<SolvedType> solvedType = solve(typeReference, globalIndex, parentIndex);
    if (!solvedType.isPresent()) {
      return null;
    }
    return findInnerClass(name, solvedType.get().getClassSymbol(), globalIndex);
  }

  @Nullable
  private ClassSymbol findClassInFile(String name, FileIndex fileIndex, GlobalIndex globalIndex) {
    Collection<Symbol> symbols = fileIndex.getMemberSymbols().get(name);
    for (Symbol symbol : symbols) {
      if (symbol instanceof ClassSymbol) {
        return (ClassSymbol) symbol;
      }
    }
    // Not declared in the file, try imported classes.
    Optional<TypeReference> importedClass = fileIndex.getImportedClass(name);
    ClassSymbol foundClass = null;
    if (importedClass.isPresent()) {
      foundClass = findClassInGlobalIndex(globalIndex, importedClass.get().getFullName());
    }
    return foundClass;
  }

  @Nullable
  private ClassSymbol findClassInPackage(String name, PackageIndex packageIndex) {
    for (Symbol symbol : packageIndex.getMemberSymbols().get(name)) {
      if (symbol instanceof ClassSymbol) {
        return (ClassSymbol) symbol;
      }
    }
    return null;
  }

  @Nullable
  private PackageIndex findPackage(GlobalIndex globalIndex, List<String> packageQualifiers) {
    PackageIndex currentIndex = globalIndex.getRootPackage();
    for (String qualifier : packageQualifiers) {
      PackageIndex nextIndex = null;
      for (Symbol symbol : currentIndex.getMemberSymbols().get(qualifier)) {
        if (symbol instanceof PackageSymbol) {
          nextIndex = (PackageIndex) symbol.getChildIndex();
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
