package org.javacomp.reference;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import com.sun.source.tree.LineMap;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.javacomp.file.FileManager;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.parser.AdjustedLineMap;
import org.javacomp.parser.PositionContext;

/** Finds references of a symbol. */
public class ReferenceSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final int MAX_SCOPES_TO_SEARCH = 500;

  private final FileManager fileManager;
  private final DefinitionSolver definitionSolver;

  public ReferenceSolver(FileManager fileManager) {
    this.fileManager = fileManager;
    this.definitionSolver = new DefinitionSolver();
  }

  public Multimap<FileScope, Range<Integer>> findReferences(
      Module module, Path filePath, int line, int column) {
    Optional<PositionContext> positionContext =
        PositionContext.createForPosition(module, filePath, line, column);
    if (!positionContext.isPresent()) {
      return ImmutableMultimap.of();
    }
    Optional<Entity> entity = findEntityWithNameAtPosition(positionContext.get());
    if (!entity.isPresent()) {
      // Not at any definition of entity. Try to get definition of the symbol at point.
      List<? extends Entity> definitions =
          new DefinitionSolver().getDefinitionEntities(module, positionContext.get());
      if (!definitions.isEmpty()) {
        entity = Optional.of(definitions.get(0));
      }
    }
    if (!entity.isPresent()) {
      return ImmutableMultimap.of();
    }
    return findReferencesForEntity(module, entity.get(), positionContext.get());
  }

  /** Finds the {@link Entity} whose name is defined a given position. */
  private Optional<Entity> findEntityWithNameAtPosition(PositionContext positionContext) {
    EntityScope scope = positionContext.getScopeAtPosition();
    Optional<Entity> entity = scope.getDefiningEntity();
    if (entity.isPresent()
        && entity.get().getSymbolRange().contains(positionContext.getPosition())) {
      return entity;
    }
    return Optional.empty();
  }

  private Multimap<FileScope, Range<Integer>> findReferencesForEntity(
      Module module, Entity entity, PositionContext positionContext) {
    ImmutableMultimap.Builder<FileScope, Range<Integer>> builder =
        new ImmutableMultimap.Builder<>();
    if (entity instanceof ClassEntity) {
      findClassConstructors(builder, (ClassEntity) entity);
    }

    // Try to limit the search of reference in a private scope.
    List<? extends EntityScope> searchScopes = findSearchScopes(module, entity, positionContext);
    for (EntityScope searchScope : searchScopes) {
      findReferencesInScope(builder, module, entity, searchScope, positionContext);
    }

    return builder.build();
  }

  private List<? extends EntityScope> findSearchScopes(
      Module module, Entity entity, PositionContext positionContext) {
    Optional<EntityScope> definitionScope = entity.getParentScope();
    checkState(definitionScope.isPresent(), "Entity %s does not have definition scope", entity);
    if (definitionScope.get() instanceof FileScope) {
      // Top level class.
      // TODO: limit search based on access level.
      return module.getAllFiles();
    } else if (!definitionScope.get().getDefiningEntity().isPresent()
        || !(definitionScope.get().getDefiningEntity().get() instanceof ClassEntity)) {
      // Not a direct member of a class. A local variable.
      return ImmutableList.of(definitionScope.get());
    } else {
      // TODO: limit search based on access level.
      return module.getAllFiles();
    }
  }

  private void findClassConstructors(
      ImmutableMultimap.Builder<FileScope, Range<Integer>> builder, ClassEntity classEntity) {
    FileScope fileScope = getDefiningFileScope((Entity) classEntity);
    for (MethodEntity constructor : classEntity.getConstructors()) {
      builder.put(fileScope, constructor.getSymbolRange());
    }
  }

  private void findReferencesInScope(
      ImmutableMultimap.Builder<FileScope, Range<Integer>> builder,
      Module module,
      Entity entity,
      EntityScope entityScope,
      PositionContext positionContext) {
    String entityName = entity.getSimpleName();
    if (entityName.isEmpty()) {
      return;
    }
    FileScope fileScope = getDefiningFileScope(entityScope);
    Optional<CharSequence> fileContent =
        fileManager.getFileContent(Paths.get(fileScope.getFilename()));
    if (!fileContent.isPresent()) {
      return;
    }
    String actualFileContent = fileContent.get().toString();
    int start = entityScope.getDefinitionRange().lowerEndpoint();
    int end = entityScope.getDefinitionRange().upperEndpoint();
    logger.fine(
        "Finding entity %s in scope %s [%s, %s] of file %s",
        entityName, entityScope, start, end, fileScope.getFilename());
    while (start < end) {
      start = actualFileContent.indexOf(entity.getSimpleName(), start);
      if (start < 0 || start >= end) {
        break;
      }
      // We have string match now. Check if the match is actually referencing
      // the entity.
      //
      // TODO(chencaibin): Add semantic checks.
      Range<Integer> referenceRange =
          createRangeForUnfixedContent(fileScope, start, entityName.length());
      if (isExactEntityName(actualFileContent, start, start + entityName.length())
          && isSameEntity(entity, module, fileScope, referenceRange.lowerEndpoint())) {
        builder.put(fileScope, referenceRange);
      }
      start += entityName.length();
    }
  }

  /**
   * Return true if the {@code content} starting from {@code start} to {@code end} is not a
   * substring of a longer indentifier.
   */
  private static boolean isExactEntityName(String content, int start, int end) {
    char beforeStart = (start > 0) ? content.charAt(start - 1) : ' ';
    char afterEnd = (end < content.length()) ? content.charAt(end) : ' ';
    return !Character.isJavaIdentifierPart(beforeStart)
        && !Character.isJavaIdentifierPart(afterEnd);
  }

  private boolean isSameEntity(Entity entity, Module module, FileScope fileScope, int start) {
    int position = start + 1; // make sure the position is inside of the entity name.
    PositionContext referenceContext =
        PositionContext.createForFixedPosition(module, fileScope, position);
    List<? extends Entity> definitions =
        definitionSolver.getDefinitionEntities(module, referenceContext);
    if (definitions.isEmpty()) {
      return false;
    }

    Entity definition = definitions.get(0);
    if ((entity instanceof ClassEntity)
        && (definition instanceof MethodEntity)
        && ((MethodEntity) definition).isConstructor()) {
      // Constructor is also considered reference to the class.
      definition = ((MethodEntity) definition).getParentClass();
    }
    return entity == definition;
  }

  private Range<Integer> createRangeForUnfixedContent(
      FileScope fileScope, int unfixedStart, int length) {
    Optional<LineMap> lineMap = fileScope.getLineMap();
    checkState(lineMap.isPresent(), "No line map for file %s", fileScope.getFilename());
    int unfixedEnd = unfixedStart + length;
    return Range.closed(
        getFixedPosition(lineMap.get(), unfixedStart), getFixedPosition(lineMap.get(), unfixedEnd));
  }

  private int getFixedPosition(LineMap lineMap, int unfixedPosition) {
    if (!(lineMap instanceof AdjustedLineMap)) {
      // The file content is not changed by FileContentFixer.
      return unfixedPosition;
    }
    AdjustedLineMap adjustedLineMap = (AdjustedLineMap) lineMap;
    LineMap originalLineMap = adjustedLineMap.getOriginalLineMap();
    int line = (int) originalLineMap.getLineNumber(unfixedPosition);
    int column = (int) originalLineMap.getColumnNumber(unfixedPosition);
    return (int) adjustedLineMap.getPosition(line, column);
  }

  private static FileScope getDefiningFileScope(Entity entity) {
    Optional<EntityScope> scope = entity.getParentScope();
    checkState(scope.isPresent());
    return getDefiningFileScope(scope.get());
  }

  private static FileScope getDefiningFileScope(EntityScope scope) {
    while (!(scope instanceof FileScope)) {
      checkState(scope.getParentScope().isPresent());
      scope = scope.getParentScope().get();
    }
    return (FileScope) scope;
  }
}
