package org.javacomp.completion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.PackageScope;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

/** An action that returns any visible entities as completion candidates. */
class CompleteEntityAction implements CompletionAction {
  private final ClassMemberCompletor classMemberCompletor;

  CompleteEntityAction(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
    this.classMemberCompletor = new ClassMemberCompletor(typeSolver, expressionSolver);
  }

  @Override
  public List<CompletionCandidate> getCompletionCandidates(
      GlobalScope globalScope, EntityScope baseScope) {
    Multimap<String, CompletionCandidate> candidateMap = HashMultimap.create();
    for (EntityScope currentScope = baseScope;
        currentScope != null;
        currentScope = currentScope.getParentScope().orElse(null)) {
      if (currentScope instanceof ClassEntity) {
        addEntries(
            candidateMap,
            classMemberCompletor.getClassMembers((ClassEntity) currentScope, globalScope));
      } else if (currentScope instanceof FileScope) {
        FileScope fileScope = (FileScope) currentScope;
        addEntries(candidateMap, getPackageMembers(fileScope, globalScope));
        addImportedEntities(candidateMap, fileScope);
      } else {
        addEntries(candidateMap, currentScope.getMemberEntities());
      }
    }
    addEntries(candidateMap, globalScope.getAllEntities());
    return ImmutableList.copyOf(candidateMap.values());
  }

  private Multimap<String, Entity> getPackageMembers(FileScope fileScope, GlobalScope globalScope) {
    PackageScope packageScope = globalScope.getPackageForFile(fileScope);
    return packageScope.getMemberEntities();
  }

  private void addImportedEntities(
      Multimap<String, CompletionCandidate> candidateMap, FileScope fileScope) {
    for (List<String> fullClassName : fileScope.getAllImportedClasses()) {
      String simpleName = fullClassName.get(fullClassName.size() - 1);
      if (!candidateMap.containsKey(simpleName)) {
        candidateMap.put(
            simpleName,
            CompletionCandidate.builder()
                .setName(simpleName)
                .setKind(CompletionCandidate.Kind.CLASS)
                .build());
      }
    }
    // TODO: support on-demand imports and static imports.
  }

  private void addEntries(
      Multimap<String, CompletionCandidate> target, Multimap<String, Entity> source) {
    for (Map.Entry<String, Entity> entry : source.entries()) {
      if (entryExist(target, entry)) {
        continue;
      }
      target.put(entry.getKey(), new EntityCompletionCandidate(entry.getValue()));
    }
  }

  private boolean entryExist(
      Multimap<String, CompletionCandidate> candidateMap, Map.Entry<String, Entity> entry) {
    if (!candidateMap.containsKey(entry.getKey())) {
      return false;
    }

    if (entry.getValue().getKind() == Entity.Kind.METHOD) {
      // Method overloads don't conflict with each other.
      //
      // TODO: Handler non overloading cases: shadowing static imports, overriding base class methods
      return false;
    }

    for (CompletionCandidate candidate : candidateMap.get(entry.getKey())) {
      if (candidate.getKind()
          == EntityCompletionCandidate.toCandidateKind(entry.getValue().getKind())) {
        return true;
      }
    }

    return false;
  }
}
