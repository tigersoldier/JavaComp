package org.javacomp.completion;

import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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

  Multimap<String, Entity> getClassMembers(EntityWithContext actualClass, Module module) {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    for (EntityWithContext classInHierachy : typeSolver.classHierarchy(actualClass, module)) {
      checkState(
          classInHierachy.getEntity() instanceof ClassEntity,
          "classHierarchy() returns non class entity %s for %s",
          classInHierachy,
          actualClass);
      if (actualClass.isInstanceContext()) {
        builder.putAll(((ClassEntity) classInHierachy.getEntity()).getMemberEntities());
      } else {
        // Non instance context only allows non instance members.
        for (Entity member :
            ((ClassEntity) classInHierachy.getEntity()).getMemberEntities().values()) {
          if (!member.isInstanceMember()) {
            builder.put(member.getSimpleName(), member);
          }
        }
      }
    }
    return builder.build();
  }
}
