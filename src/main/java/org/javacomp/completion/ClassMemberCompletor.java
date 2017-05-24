package org.javacomp.completion;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.ModuleScope;
import org.javacomp.typesolver.ExpressionSolver;
import org.javacomp.typesolver.TypeSolver;

class ClassMemberCompletor {
  private final TypeSolver typeSolver;
  private final ExpressionSolver expressionSolver;

  ClassMemberCompletor(TypeSolver typeSolver, ExpressionSolver expressionSolver) {
    this.typeSolver = typeSolver;
    this.expressionSolver = expressionSolver;
  }

  Multimap<String, Entity> getClassMembers(ClassEntity actualClassEntity, ModuleScope globalScope) {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    for (ClassEntity classEntity : typeSolver.classHierarchy(actualClassEntity, globalScope)) {
      builder.putAll(classEntity.getMemberEntities());
    }
    return builder.build();
  }
}
