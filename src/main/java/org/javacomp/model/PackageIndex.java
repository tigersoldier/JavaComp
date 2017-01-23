package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Index of sub packages and files in a package. */
public class PackageIndex implements EntityIndex {
  // Map of simple names -> subPackages.
  private final Multimap<String, PackageEntity> subPackages;
  private final Set<FileIndex> files;

  public PackageIndex() {
    this.subPackages = HashMultimap.create();
    this.files = new HashSet<>();
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(subPackages.get(simpleName));
    for (FileIndex fileIndex : files) {
      builder.addAll(fileIndex.getEntitiesWithName(simpleName));
    }
    return builder.build();
  }

  @Override
  public Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind) {
    for (Entity entity : subPackages.get(simpleName)) {
      if (entity.getKind() == entityKind) {
        return Optional.of(entity);
      }
    }
    for (FileIndex fileIndex : files) {
      Optional<Entity> entity = fileIndex.getEntityWithNameAndKind(simpleName, entityKind);
      if (entity.isPresent()) {
        return entity;
      }
    }
    return Optional.absent();
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(subPackages);
    for (FileIndex fileIndex : files) {
      builder.putAll(fileIndex.getAllEntities());
    }
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(subPackages);
    for (FileIndex fileIndex : files) {
      builder.putAll(fileIndex.getMemberEntities());
    }
    return builder.build();
  }

  @Override
  public void addEntity(Entity entity) {
    checkArgument(
        entity instanceof PackageEntity,
        "Only sub package can be added to a package. Found " + entity.getClass().getSimpleName());
    subPackages.put(entity.getSimpleName(), (PackageEntity) entity);
  }

  public void removePackage(PackageEntity entity) {
    subPackages.remove(entity.getSimpleName(), entity);
  }

  public void addFile(FileIndex fileIndex) {
    files.add(fileIndex);
  }

  public void removeFile(FileIndex fileIndex) {
    files.remove(fileIndex);
  }

  /** @return whether the package has sub packages or files. */
  public boolean hasChildren() {
    return !(subPackages.isEmpty() && files.isEmpty());
  }

  @Override
  public Optional<EntityIndex> getParentIndex() {
    return Optional.absent();
  }
}
