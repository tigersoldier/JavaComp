package org.javacomp.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.RangeMap;
import com.sun.source.tree.LineMap;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nullable;

/** Scope of entities in the scope of a Java source file. */
public class FileScope implements EntityScope {
  private final String filename;
  // Map of simple names -> entities.
  private final Multimap<String, Entity> entities;
  // Simples that can be reached globally.
  // Map of simple names -> entities.
  private final Multimap<String, Entity> globalEntities;
  private final ImmutableList<String> packageQualifiers;
  private final Map<String, List<String>> importedClasses;
  private final List<List<String>> onDemandClassImportQualifiers;
  private final JCCompilationUnit compilationUnit;
  private RangeMap<Integer, EntityScope> scopeRangeMap = null;
  private LineMap adjustedLineMap = null;

  public FileScope(
      String filename, List<String> packageQualifiers, JCCompilationUnit compilationUnit) {
    this.filename = filename;
    this.entities = HashMultimap.create();
    this.packageQualifiers = ImmutableList.copyOf(packageQualifiers);
    this.globalEntities = HashMultimap.create();
    this.importedClasses = new HashMap<>();
    this.onDemandClassImportQualifiers = new ArrayList<>();
    this.compilationUnit = compilationUnit;
  }

  @Override
  public Multimap<String, Entity> getMemberEntities() {
    return ImmutableMultimap.copyOf(entities);
  }

  public Optional<List<String>> getImportedClass(String simpleName) {
    return Optional.ofNullable(importedClasses.get(simpleName));
  }

  public List<List<String>> getAllImportedClasses() {
    return ImmutableList.copyOf(importedClasses.values());
  }

  public void addImportedClass(List<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return;
    }
    importedClasses.put(qualifiers.get(qualifiers.size() - 1), qualifiers);
  }

  /**
   * Returns a list of all on-demand class imported qualifiers added by {@link
   * #addOnDemandClassImport}.
   *
   * <p>Similar to {@link #addOnDemandClassImport}, the returned qualifiers do not include the
   * trailing *.
   */
  public List<List<String>> getOnDemandClassImportQualifiers() {
    return ImmutableList.copyOf(onDemandClassImportQualifiers);
  }

  /**
   * Adds an on-demand class import (e.g. {@code import foo.bar.*}).
   *
   * @param qualifiers the imported package qualifiers without *. For example, if the import
   *     statment is {@code import foo.bar.*}, then the qualifiers are {@code ['foo', 'bar']}
   */
  public void addOnDemandClassImport(List<String> qualifiers) {
    if (qualifiers.isEmpty()) {
      return;
    }
    onDemandClassImportQualifiers.add(ImmutableList.copyOf(qualifiers));
  }

  @Override
  public void addEntity(Entity entity) {
    entities.put(entity.getSimpleName(), entity);
  }

  public void setScopeRangeMap(RangeMap<Integer, EntityScope> scopeRangeMap) {
    this.scopeRangeMap = scopeRangeMap;
  }

  public RangeMap<Integer, EntityScope> getScopeRangeMap() {
    return scopeRangeMap;
  }

  @Nullable
  public EntityScope getEntityScopeAt(int position) {
    return scopeRangeMap.get(position);
  }

  public void addGlobalEntity(Entity entity) {
    globalEntities.put(entity.getSimpleName(), entity);
  }

  /** @return a multimap of entity simple name to entities */
  public Multimap<String, Entity> getGlobalEntities() {
    return ImmutableMultimap.copyOf(globalEntities);
  }

  public List<Entity> getGlobalEntitiesWithName(String simpleName) {
    return ImmutableList.copyOf(globalEntities.get(simpleName));
  }

  public List<String> getPackageQualifiers() {
    return packageQualifiers;
  }

  @Override
  public Optional<EntityScope> getParentScope() {
    return Optional.empty();
  }

  public String getFilename() {
    return filename;
  }

  public JCCompilationUnit getCompilationUnit() {
    return compilationUnit;
  }

  public void setAdjustedLineMap(LineMap adjustedLineMap) {
    this.adjustedLineMap = adjustedLineMap;
  }

  /**
   * Gets the {@link LineMap} for this file.
   *
   * <p>Note: use this method instead of {@code getCompilationUnit().getLineMap()}. The line map may
   * need adjustment if the source code is fixed by {@code FileContentFixer}.
   */
  public LineMap getLineMap() {
    if (adjustedLineMap != null) {
      return adjustedLineMap;
    }
    return compilationUnit.getLineMap();
  }

  @Override
  public String toString() {
    return "FileScope<" + getFilename() + ", " + this.packageQualifiers + ">";
  }
}
