package org.javacomp.completion;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import org.javacomp.completion.CompletionCandidate.SortCategory;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.Module;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

class ClassMemberCompletor {
  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;

  ClassMemberCompletor(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
    this.typeSolver = typeSolver;
    this.expressionSolver = expressionSolver;
  }

  ImmutableList<CompletionCandidate> getClassMembers(
      EntityWithContext actualClass,
      Module module,
      String prefix,
      boolean addBothInstanceAndStaticMembers) {
    CompletionCandidateListBuilder builder = new CompletionCandidateListBuilder(prefix);
    boolean directMembers = true;
    for (EntityWithContext classInHierachy : typeSolver.classHierarchy(actualClass, module)) {
      checkState(
          classInHierachy.getEntity() instanceof ClassEntity,
          "classHierarchy() returns non class entity %s for %s",
          classInHierachy,
          actualClass);
      for (Entity member :
          ((ClassEntity) classInHierachy.getEntity()).getMemberEntities().values()) {
        if (addBothInstanceAndStaticMembers
            || actualClass.isInstanceContext() == member.isInstanceMember()) {
          builder.addEntity(
              member, directMembers ? SortCategory.DIRECT_MEMBER : SortCategory.ACCESSIBLE_SYMBOL);
        }
      }
      directMembers = false;
    }
    return builder.build();
  }
}
