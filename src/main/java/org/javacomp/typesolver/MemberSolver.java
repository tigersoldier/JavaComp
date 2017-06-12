package org.javacomp.typesolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityWithContext;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedArrayType;
import org.javacomp.model.SolvedEntityType;
import org.javacomp.model.SolvedReferenceType;
import org.javacomp.model.SolvedType;
import org.javacomp.model.VariableEntity;

/** Logic for finding the entity that defines the member of a class. */
public class MemberSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

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
      String identifier, SolvedType baseType, Module module) {
    return findNonMethodMember(identifier, baseType, module, ALLOWED_KINDS_NON_METHOD);
  }

  public Optional<EntityWithContext> findNonMethodMember(
      String identifier, SolvedType baseType, Module module, Set<Entity.Kind> allowedKinds) {
    ///////
    // OuterClass.this
    if (baseType instanceof SolvedReferenceType && IDENT_THIS.equals(identifier)) {
      return Optional.of(EntityWithContext.from(baseType).build());
    }

    ////////
    //  someArray.length
    if (baseType instanceof SolvedArrayType && IDENT_LENGTH.equals(identifier)) {
      return Optional.of(EntityWithContext.simpleBuilder().setEntity(PrimitiveEntity.INT).build());
    }

    ////////
    //  foo.bar
    if (baseType instanceof SolvedEntityType) {
      return typeSolver
          .findEntityMember(
              identifier, ((SolvedEntityType) baseType).getEntity(), module, allowedKinds)
          .map(entity -> EntityWithContext.from(baseType).setEntity(entity).build());
    }
    return Optional.empty();
  }

  /**
   * @return a list of {@link MethodEntity} instances. The best match is the first element if not
   *     empty.
   */
  public List<EntityWithContext> findMethodMembers(
      String identifier, List<Optional<SolvedType>> arguments, SolvedType baseType, Module module) {
    // Methods must be defined in classes.
    if (!(baseType instanceof SolvedReferenceType)) {
      logger.warning(new Throwable(), "Cannot find method of non-class entities %s", baseType);
      return ImmutableList.of();
    }

    List<Entity> methodEntities =
        typeSolver.findClassMethods(
            identifier, ((SolvedReferenceType) baseType).getEntity(), module);
    if (methodEntities.isEmpty()) {
      return ImmutableList.of();
    }

    return overloadSolver
        .prioritizeMatchedMethod(methodEntities, arguments, module)
        .stream()
        .map(entity -> EntityWithContext.from(baseType).setEntity(entity).build())
        .collect(ImmutableList.toImmutableList());
  }
}
