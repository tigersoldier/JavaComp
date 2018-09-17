package org.javacomp.reference;

import static com.google.common.base.Preconditions.checkState;

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
import org.javacomp.model.Module;
import org.javacomp.parser.AdjustedLineMap;
import org.javacomp.parser.PositionContext;

/** Finds references of a symbol. */
public class ReferenceSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final FileManager fileManager;

  public ReferenceSolver(FileManager fileManager) {
    this.fileManager = fileManager;
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
    // Try to limit the search of reference in a private scope.
    Optional<EntityScope> definitionScope = entity.getParentScope();
    checkState(definitionScope.isPresent(), "Entity %s does not have definition scope", entity);
    if (definitionScope.get() instanceof FileScope) {
      // Top level class.
      // TODO: real implementation.
      findReferencesInScope(
          builder, module, entity, positionContext.getFileScope(), positionContext);
    } else if (!definitionScope.get().getDefiningEntity().isPresent()
        || !(definitionScope.get().getDefiningEntity().get() instanceof ClassEntity)) {
      // Not a direct member of a class. A local variable.
      findReferencesInScope(builder, module, entity, definitionScope.get(), positionContext);
    } else {
      // TODO: real implementation.
      findReferencesInScope(
          builder, module, entity, positionContext.getFileScope(), positionContext);
    }

    return builder.build();
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
    FileScope fileScope = positionContext.getFileScope();
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
      if (start >= 0 && start < end) {
        // TODO(chencaibin): Check that this is the actual reference.
        builder.put(fileScope, createRangeForUnfixedContent(fileScope, start, entityName.length()));
        start += entityName.length();
      } else {
        break;
      }
    }
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
}
