package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The index of the whole project. Can reach all scoped entities (e.g. packages, classes) defined in
 * the project.
 */
public class GlobalIndex implements EntityIndex {
  // Map of simple names -> FileIndex that defines the name.
  private final Multimap<String, FileIndex> nameToFileMap;
  // Map of filename -> FileIndex.
  private final Map<String, FileIndex> fileIndexMap;
  private final PackageIndex rootPackage;

  public GlobalIndex() {
    this.nameToFileMap = HashMultimap.create();
    this.fileIndexMap = new HashMap<>();
    this.rootPackage = new PackageIndex();
  }

  @Override
  public List<Entity> getEntitiesWithName(final String simpleName) {
    return FluentIterable.from(nameToFileMap.get(simpleName))
        .transformAndConcat(fileIndex -> fileIndex.getGlobalEntitiesWithName(simpleName))
        .append(rootPackage.getEntitiesWithName(simpleName))
        .toList();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    for (Entity entity : getEntitiesWithName(simpleName)) {
      if (entity.getKind() == entityKind) {
        return Optional.of(entity);
      }
    }
    return Optional.absent();
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    return FluentIterable.from(fileIndexMap.values())
        .transformAndConcat(fileIndex -> fileIndex.getGlobalEntities().values())
        .append(rootPackage.getAllEntities().values())
        .index(entity -> entity.getSimpleName());
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    return rootPackage.getMemberEntities();
  }

  @Override
  public void addEntity(Entity entity) {
    throw new UnsupportedOperationException();
  }

  public void addOrReplaceFileIndex(FileIndex fileIndex) {
    FileIndex existingFileIndex = fileIndexMap.get(fileIndex.getFilename());
    // Add the new file index to the package first, so that we don't GC the pacakge if
    // the new file and old file are in the same pacakge and is the only file in the package.
    addFileToPackage(fileIndex);

    if (existingFileIndex != null) {
      // Remove old entity indexes.
      for (String entityName : existingFileIndex.getGlobalEntities().keys()) {
        nameToFileMap.remove(entityName, existingFileIndex);
      }
      removeFileFromPacakge(existingFileIndex);
    }
    fileIndexMap.put(fileIndex.getFilename(), fileIndex);
    for (String entityName : fileIndex.getGlobalEntities().keys()) {
      nameToFileMap.put(entityName, fileIndex);
    }
  }

  public Optional<FileIndex> getFileIndex(String filename) {
    return Optional.fromNullable(fileIndexMap.get(filename));
  }

  public PackageIndex getRootPackage() {
    return rootPackage;
  }

  private void addFileToPackage(FileIndex fileIndex) {
    List<String> currentQualifiers = new ArrayList<>();
    PackageIndex currentPackage = rootPackage;
    for (String qualifier : fileIndex.getPackageQualifiers()) {
      Optional<Entity> packageEntity =
          currentPackage.getEntityWithNameAndKind(qualifier, Entity.Kind.QUALIFIER);
      if (packageEntity.isPresent()) {
        currentPackage = ((PackageEntity) packageEntity.get()).getChildIndex();
      } else {
        PackageIndex packageIndex = new PackageIndex();
        currentPackage.addEntity(new PackageEntity(qualifier, currentQualifiers, packageIndex));
        currentPackage = packageIndex;
      }
      currentQualifiers.add(qualifier);
    }
    currentPackage.addFile(fileIndex);
  }

  private void removeFileFromPacakge(FileIndex fileIndex) {
    Deque<PackageEntity> stack = new ArrayDeque<>();
    PackageIndex currentPackage = rootPackage;
    for (String qualifier : fileIndex.getPackageQualifiers()) {
      Optional<Entity> optionalPackageEntity =
          currentPackage.getEntityWithNameAndKind(qualifier, Entity.Kind.QUALIFIER);
      if (!optionalPackageEntity.isPresent()) {
        throw new RuntimeException("Package " + qualifier + " not found");
      }
      PackageEntity packageEntity = (PackageEntity) optionalPackageEntity.get();
      stack.addFirst(packageEntity);
      currentPackage = packageEntity.getChildIndex();
    }
    currentPackage.removeFile(fileIndex);
    while (!currentPackage.hasChildren() && !stack.isEmpty()) {
      PackageEntity packageEntity = stack.removeFirst();
      currentPackage = stack.isEmpty() ? rootPackage : stack.peekFirst().getChildIndex();
      currentPackage.removePackage(packageEntity);
    }
  }

  @Override
  public Optional<EntityIndex> getParentIndex() {
    return Optional.absent();
  }
}
