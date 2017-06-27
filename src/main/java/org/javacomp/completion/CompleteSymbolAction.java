package org.javacomp.completion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PackageScope;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.PositionContext;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

/** An action that returns any visible entities as completion candidates. */
class CompleteEntityAction implements CompletionAction {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final List<String> JAVA_LANG_QUALIFIERS = ImmutableList.of("java", "lang");
  private static final Set<Entity.Kind> METHOD_VARIABLE_KINDS =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(VariableEntity.ALLOWED_KINDS)
          .add(Entity.Kind.METHOD)
          .build();

  private final TypeSolver typeSolver;
  private final ClassMemberCompletor classMemberCompletor;

  CompleteEntityAction(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
    this.typeSolver = typeSolver;
    this.classMemberCompletor = new ClassMemberCompletor(typeSolver, expressionSolver);
  }

  @Override
  public List<CompletionCandidate> getCompletionCandidates(PositionContext positionContext) {
    Multimap<String, CompletionCandidate> candidateMap = HashMultimap.create();
    addKeywords(candidateMap);
    for (EntityScope currentScope = positionContext.getScopeAtPosition();
        currentScope != null;
        currentScope = currentScope.getParentScope().orElse(null)) {
      logger.fine("Adding member entities in scope: %s", currentScope);
      if (currentScope instanceof ClassEntity) {
        addEntries(
            candidateMap,
            classMemberCompletor.getClassMembers(
                EntityWithContext.ofEntity((ClassEntity) currentScope),
                positionContext.getModule()));
      } else if (currentScope instanceof FileScope) {
        FileScope fileScope = (FileScope) currentScope;
        addEntries(candidateMap, getPackageMembers(fileScope, positionContext.getModule()));
        addImportedEntities(candidateMap, fileScope, positionContext.getModule());
      } else {
        addEntries(candidateMap, currentScope.getMemberEntities());
      }
    }
    addEntries(
        candidateMap,
        typeSolver.getAggregateRootPackageScope(positionContext.getModule()).getMemberEntities());

    Optional<PackageScope> javaLangPackage =
        typeSolver.findPackageInModule(JAVA_LANG_QUALIFIERS, positionContext.getModule());
    if (javaLangPackage.isPresent()) {
      addEntries(candidateMap, javaLangPackage.get().getMemberEntities());
    }

    return ImmutableList.copyOf(candidateMap.values());
  }

  private Multimap<String, Entity> getPackageMembers(FileScope fileScope, Module module) {
    PackageScope packageScope = module.getPackageForFile(fileScope);
    return packageScope.getMemberEntities();
  }

  private void addImportedEntities(
      Multimap<String, CompletionCandidate> candidateMap, FileScope fileScope, Module module) {
    // import foo.Bar;
    for (List<String> fullClassName : fileScope.getAllImportedClasses()) {
      String simpleName = fullClassName.get(fullClassName.size() - 1);
      if (!candidateMap.containsKey(simpleName)) {
        candidateMap.put(
            simpleName,
            SimpleCompletionCandidate.builder()
                .setName(simpleName)
                .setKind(CompletionCandidate.Kind.CLASS)
                .build());
      }
    }

    // import static foo.Bar.BAZ;
    for (List<String> fullMemberName : fileScope.getAllImportedStaticMembers()) {
      ClassEntity enclosingClass =
          typeSolver.solveClassOfStaticImport(fullMemberName, fileScope, module).orElse(null);
      if (enclosingClass == null) {
        continue;
      }
      String name = fullMemberName.get(fullMemberName.size() - 1);
      for (Entity member : enclosingClass.getMemberEntities().get(name)) {
        if (!member.isStatic()) {
          continue;
        }

        if (member instanceof MethodEntity || member instanceof VariableEntity) {
          addEntity(candidateMap, member);
        }
      }
    }

    // import foo.Bar.*;
    addOnDemandImportedEntities(
        candidateMap,
        fileScope.getOnDemandClassImportQualifiers(),
        module,
        ClassEntity.ALLOWED_KINDS);

    // import static foo.Bar.*;
    addOnDemandImportedEntities(
        candidateMap, fileScope.getOnDemandStaticImportQualifiers(), module, METHOD_VARIABLE_KINDS);
  }

  private void addOnDemandImportedEntities(
      Multimap<String, CompletionCandidate> candidateMap,
      List<List<String>> importedQualifiers,
      Module module,
      Set<Entity.Kind> allowedKinds) {
    for (List<String> qualifiers : importedQualifiers) {
      Entity enclosingClassOrPackage =
          typeSolver.findClassOrPackageInModule(qualifiers, module).orElse(null);
      if (enclosingClassOrPackage == null) {
        continue;
      }

      for (Entity member : enclosingClassOrPackage.getChildScope().getMemberEntities().values()) {
        if (member.isStatic() && allowedKinds.contains(member.getKind())) {
          addEntity(candidateMap, member);
        }
      }
    }
  }

  private void addKeywords(Multimap<String, CompletionCandidate> candidateMap) {
    // TODO: add only keywords that are available for the current context.
    for (KeywordCompletionCandidate keyword : KeywordCompletionCandidate.values()) {
      candidateMap.put(keyword.getName(), keyword);
    }
  }

  private void addEntries(
      Multimap<String, CompletionCandidate> target, Multimap<String, Entity> source) {
    for (Entity entity : source.values()) {
      addEntity(target, entity);
    }
  }

  private void addEntity(Multimap<String, CompletionCandidate> target, Entity entity) {
    if (entityExist(target, entity)) {
      return;
    }
    target.put(entity.getSimpleName(), new EntityCompletionCandidate(entity));
  }

  private boolean entityExist(Multimap<String, CompletionCandidate> candidateMap, Entity entity) {
    if (!candidateMap.containsKey(entity.getSimpleName())) {
      return false;
    }

    if (entity.getKind() == Entity.Kind.METHOD) {
      // Method overloads don't conflict with each other.
      //
      // TODO: Handler non overloading cases: shadowing static imports, overriding base class methods
      return false;
    }

    for (CompletionCandidate candidate : candidateMap.get(entity.getSimpleName())) {
      if (candidate.getKind() == EntityCompletionCandidate.toCandidateKind(entity.getKind())) {
        return true;
      }
    }

    return false;
  }
}
