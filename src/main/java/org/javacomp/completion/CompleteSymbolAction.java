package org.javacomp.completion;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
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
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope baseScope) {
    Multimap<String, Entity> entityMap = HashMultimap.create();
    for (EntityScope currentScope = baseScope;
        currentScope != null;
        currentScope = currentScope.getParentScope().orElse(null)) {
      if (currentScope instanceof ClassEntity) {
        addEntries(
            entityMap,
            classMemberCompletor.getClassMembers((ClassEntity) currentScope, globalScope));
      } else if (currentScope instanceof FileScope) {
        addEntries(entityMap, getPackageMembers((FileScope) currentScope, globalScope));
      } else {
        addEntries(entityMap, currentScope.getMemberEntities());
      }
    }
    entityMap.putAll(globalScope.getAllEntities());
    return entityMap;
  }

  private Multimap<String, Entity> getPackageMembers(FileScope fileScope, GlobalScope globalScope) {
    PackageScope packageScope = globalScope.getPackageForFile(fileScope);
    return packageScope.getMemberEntities();
  }

  private void addEntries(Multimap<String, Entity> target, Multimap<String, Entity> source) {
    for (Map.Entry<String, Entity> entry : source.entries()) {
      if (entryExist(target, entry)) {
        continue;
      }
      target.put(entry.getKey(), entry.getValue());
    }
  }

  private boolean entryExist(Multimap<String, Entity> entityMap, Map.Entry<String, Entity> entry) {
    if (!entityMap.containsKey(entry.getKey())) {
      return false;
    }

    if (entry.getValue().getKind() == Entity.Kind.METHOD) {
      // Method overloads don't conflict with each other.
      //
      // TODO: Handler non overloading cases: shadowing static imports, overriding base class methods
      return false;
    }

    for (Entity entity : entityMap.get(entry.getKey())) {
      if (entity.getClass() == entry.getValue().getClass()) {
        return true;
      }
    }

    return false;
  }
}
