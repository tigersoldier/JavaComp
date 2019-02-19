package org.javacomp.typesolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.StackSize;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.VariableEntity;

/** Logic for finding the entity that defines the member of a class. */
public class MemberSolver {
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private static final String IDENT_THIS = "this";
  private static final String IDENT_LENGTH = "length";
  private static final Set<Entity.Kind> ALLOWED_KINDS_NON_METHOD =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .addAll(VariableEntity.ALLOWED_KINDS)
          .add(Entity.Kind.QUALIFIER)
          .build();

  private final TypeSolver typeSolver;
  private final OverloadSolver overloadSolver;

  public MemberSolver(TypeSolver typeSolver, OverloadSolver overloadSolver) {
    this.typeSolver = typeSolver;
    this.overloadSolver = overloadSolver;
  }

  public Optional<EntityWithContext> findNonMethodMember(
      String identifier, EntityWithContext baseEntity, Module module) {
    return findNonMethodMember(identifier, baseEntity, module, ALLOWED_KINDS_NON_METHOD);
  }

  public Optional<EntityWithContext> findNonMethodMember(
      String identifier,
      EntityWithContext baseEntity,
      Module module,
      Set<Entity.Kind> allowedKinds) {
    ////////
    // Array
    if (baseEntity.getArrayLevel() > 0) {
      if (IDENT_LENGTH.equals(identifier)) {
        return Optional.of(EntityWithContext.ofStaticEntity(PrimitiveEntity.INT));
      }
      return Optional.empty();
    }

    ///////
    // OuterClass.this
    if (baseEntity.getEntity() instanceof ClassEntity
        && !baseEntity.isInstanceContext()
        && IDENT_THIS.equals(identifier)) {
      return Optional.of(
          baseEntity
              .toBuilder()
              .setInstanceContext(true)
              .setSolvedTypeParameters(
                  typeSolver.solveTypeParameters(
                      ((ClassEntity) baseEntity.getEntity()).getTypeParameters(),
                      ImmutableList.<TypeArgument>of(),
                      baseEntity.getSolvedTypeParameters(),
                      baseEntity.getEntity().getScope(),
                      module))
              .build());
    }

    ////////
    //  foo.bar
    return typeSolver.findEntityMember(identifier, baseEntity, module, allowedKinds);
  }

  /** @return a list of {@link MethodEntity} instances. */
  public List<EntityWithContext> findMethodMembers(
      String identifier, EntityWithContext baseEntity, Module module) {
    // Methods must be defined in classes.
    if (!(baseEntity.getEntity() instanceof ClassEntity)) {
      logger.atWarning().withStackTrace(StackSize.LARGE).log(
          "Cannot find method of non-class entities %s", baseEntity);
      return ImmutableList.of();
    }

    return typeSolver.findClassMethods(identifier, baseEntity, module);
  }
}
