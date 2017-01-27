package org.javacomp.model;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Scope of sub packages and files in a package. */
public class PackageScope implements EntityScope {
  // Map of simple names -> subPackages.
  private final Multimap<String, PackageEntity> subPackages;
  private final Set<FileScope> files;

  public PackageScope() {
    this.subPackages = HashMultimap.create();
    this.files = new HashSet<>();
  }

  @Override
  public List<Entity> getEntitiesWithName(String simpleName) {
    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.addAll(subPackages.get(simpleName));
    for (FileScope fileScope : files) {
      builder.addAll(fileScope.getEntitiesWithName(simpleName));
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
    for (FileScope fileScope : files) {
      Optional<Entity> entity = fileScope.getEntityWithNameAndKind(simpleName, entityKind);
      if (entity.isPresent()) {
        return entity;
      }
    }
    return Optional.empty();
  }

  @Override
  public Multimap<String, Entity> getAllEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(subPackages);
    for (FileScope fileScope : files) {
      builder.putAll(fileScope.getAllEntities());
    }
    return builder.build();
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    builder.putAll(subPackages);
    for (FileScope fileScope : files) {
      builder.putAll(fileScope.getMemberEntities());
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

  public void addFile(FileScope fileScope) {
    files.add(fileScope);
  }

  public void removeFile(FileScope fileScope) {
    files.remove(fileScope);
  }

  /** @return whether the package has sub packages or files. */
  public boolean hasChildren() {
    return !(subPackages.isEmpty() && files.isEmpty());
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.empty();
  }
}
