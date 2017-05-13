package org.javacomp.typesolver;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;

/** Logic for finding the entity that defines the member of a class. */
public class MemberSolver {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String IDENT_THIS = "this";
  private static final String IDENT_LENGTH = "length";
  private static final Set<Entity.Kind> ALLOWED_KINDS_NON_METHOD =
      new ImmutableSet.Builder<Entity.Kind>()
          .addAll(ClassEntity.ALLOWED_KINDS)
          .add(Entity.Kind.VARIABLE)
          .add(Entity.Kind.FIELD)
          .add(Entity.Kind.QUALIFIER)
          .build();

  private final TypeSolver typeSolver;
  private final OverloadSolver overloadSolver;

  public MemberSolver(TypeSolver typeSolver, OverloadSolver overloadSolver) {
    this.typeSolver = typeSolver;
    this.overloadSolver = overloadSolver;
  }

  public Optional<Entity> findNonMethodMember(
      String identifier, SolvedType baseType, GlobalScope globalScope) {
    ///////
    // OuterClass.this
    if (IDENT_THIS.equals(identifier)) {
      return Optional.of(baseType.getEntity());
    }

    ////////
    //  someArray.length
    if (baseType.isArray() && IDENT_LENGTH.equals(identifier)) {
      return Optional.of(PrimitiveEntity.INT);
    }

    ////////
    //  foo.bar
    Entity memberEntity =
        typeSolver.findEntityMember(
            identifier, baseType.getEntity(), globalScope, ALLOWED_KINDS_NON_METHOD);
    return Optional.ofNullable(memberEntity);
  }

  public Optional<MethodEntity> findMethodMember(
      String identifier,
      List<Optional<SolvedType>> arguments,
      SolvedType baseType,
      GlobalScope globalScope) {
    // Methods must be defined in classes.
    if (!(baseType.getEntity() instanceof ClassEntity)) {
      logger.warning(
          new Throwable(), "Cannot find method of non-class entities %s", baseType.getEntity());
      return Optional.empty();
    }

    List<Entity> methodEntities =
        typeSolver.findClassMethods(identifier, (ClassEntity) baseType.getEntity(), globalScope);
    if (methodEntities.isEmpty()) {
      return Optional.empty();
    }

    for (Entity entity : methodEntities) {
      checkArgument(entity instanceof MethodEntity, "Entity %s is not a method", entity);
    }

    @SuppressWarnings("unchecked")
    List<MethodEntity> methods = (List<MethodEntity>) (List) methodEntities;
    return Optional.of(overloadSolver.solve(methods, arguments, globalScope));
  }
}
