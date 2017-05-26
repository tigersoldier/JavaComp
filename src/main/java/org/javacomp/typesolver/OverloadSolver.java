package org.javacomp.typesolver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;

/**
 * Find which method overload should be invoked with given arguments
 *
 * <p>Java 8 language spec 15.12.2 for method invocation:
 * https://docs.oracle.com/javase/specs/jls/se8/html/jls-15.html#jls-15.12.2
 */
public class OverloadSolver {
  private static final String OBJECT_FULL_NAME = "java.lang.Object";

  private static final JLogger logger = JLogger.createForEnclosingClass();

  // The type specified by the key can be converted to the types specified by the values without
  // losing information about the overall magnitude.
  //
  // See Java SE 8 language spec 5.1.2. Widening Primitive Conversion:
  // https://docs.oracle.com/javase/specs/jls/se8/html/jls-5.html#jls-5.1.2
  private static final Multimap<String, String> WIDENING_PRIMITIVE_CONVERSION_MAP =
      new ImmutableSetMultimap.Builder<String, String>()
          .putAll("byte", "short", "int", "long", "float", "double")
          .putAll("short", "int", "long", "float", "double")
          .putAll("char", "int", "long", "float", "double")
          .putAll("int", "long", "float", "double")
          .putAll("long", "float", "double")
          .putAll("float", "double")
          .build();
  private static final Map<String, String> UNBOXING_MAP =
      new ImmutableMap.Builder<String, String>()
          .put("java.lang.Boolean", "boolean")
          .put("java.lang.Byte", "byte")
          .put("java.lang.Short", "short")
          .put("java.lang.Character", "char")
          .put("java.lang.Integer", "int")
          .put("java.lang.Long", "long")
          .put("java.lang.Float", "float")
          .put("java.lang.Double", "double")
          .build();

  /**
   * The level of method signagure match.
   *
   * <p>The enum values are sorted in the order from least match to most match. If the match level
   * of signagure A is defined below the match level of signagure B, signagure B should never be
   * picked.
   */
  private enum SignatureMatchLevel {
    /** Not match, not even the length of arguments. */
    LENGTH_NOT_MATCH,
    /** Length match, but type doesn't match. */
    TYPE_NOT_MATCH,
    /**
     * If consider boxing/unGboxing and variable arity, the method signagure matches. This is
     * described as phase 3 in the spec.
     */
    VARIABLE_ARITY_LOOSE_INVOCATION,
    /**
     * If consider boxing/unboxing but not varargs, the method signagure matches. This is described
     * as phase 2 in the spec.
     */
    LOOSE_INVOCATION,
    /**
     * All argument type matches without boxing/unboxing or varargs. This is described as phase 1 in
     * the spec.
     */
    STRICT_INVOCATION,
  }

  /** Defines how a parameter defined by a method matches an argument passed to it. */
  private enum TypeMatchLevel {
    /** The argument type and parameter type don't match at all. */
    NOT_MATCH,
    /** The parameter type match the argument type with the need of auto-boxing. */
    MATCH_WITH_BOXING,
    /** The parameter type match the argument type without the need of auto-boxing. */
    MATCH_WITHOUT_BOXING,
  }

  private final TypeSolver typeSolver;

  public OverloadSolver(TypeSolver typeSolver) {
    this.typeSolver = typeSolver;
  }

  /**
   * Finds a method from {@code methods} whose signature matches given {@code argumentTypes} the
   * best.
   *
   * <p>Note: unlike Java overload resolution rules, when no method matches or there are ambiguous
   * matches, it always pick one method and return it. Doing so makes it more tolerant to
   * uncompilable code which is common during development.
   *
   * @param methods a list of method overloads to be picked from. Must not be empty
   * @param argumentTypes types of arguments passed to invoke the method
   * @param module the module to solve the argument and parameter types within
   * @return one of the methods in {@code methods}
   */
  public MethodEntity solve(
      List<Entity> entities, List<Optional<SolvedType>> argumentTypes, Module module) {
    List<MethodEntity> methods =
        entities
            .stream()
            .filter(entity -> entity instanceof MethodEntity)
            .map(entity -> (MethodEntity) entity)
            .collect(ImmutableList.toImmutableList());

    checkArgument(!methods.isEmpty(), "must contain at least one method");
    if (methods.size() == 1) {
      return methods.get(0);
    }

    SignatureMatchLevel previousMatchLevel = SignatureMatchLevel.LENGTH_NOT_MATCH;
    List<MethodEntity> matchedMethods = new ArrayList<>();
    // Find the best match methods.
    for (MethodEntity method : methods) {
      SignatureMatchLevel matchLevel = matchMethodSignature(method, argumentTypes, module);
      switch (compareMatchLevel(matchLevel, previousMatchLevel)) {
        case -1:
          // The previous matched methods are better match than this method, skip it.
          continue;
        case 0:
          // This method is as good as previously matched methods. Add it the the list of matched methods.
          matchedMethods.add(method);
          break;
        case 1:
          // The method is better than previously matched methods. Clear all matched methods and add it.
          previousMatchLevel = matchLevel;
          matchedMethods.clear();
          matchedMethods.add(method);
          break;
      }
    }

    return getMostSpecificMethod(matchedMethods, argumentTypes.size(), previousMatchLevel, module);
  }

  private SignatureMatchLevel matchMethodSignature(
      MethodEntity method, List<Optional<SolvedType>> argumentTypes, Module module) {
    List<TypeReference> parameterTypes =
        method
            .getParameters()
            .stream()
            .map(p -> p.getType())
            .collect(ImmutableList.toImmutableList());

    boolean isVariableArityInvocation =
        !parameterTypes.isEmpty() && parameterTypes.get(parameterTypes.size() - 1).isArray();
    if (!isVariableArityInvocation && argumentTypes.size() != parameterTypes.size()) {
      return SignatureMatchLevel.LENGTH_NOT_MATCH;
    }
    if (argumentTypes.size() < method.getParameters().size() - 1) {
      // Too few arguments, cannot be applied to variable arity method.
      return SignatureMatchLevel.LENGTH_NOT_MATCH;
    }

    // Special case: no arguments.
    if (parameterTypes.isEmpty()) {
      return SignatureMatchLevel.STRICT_INVOCATION;
    }

    // Assuming we are at the highest possible match level. Downgrade once we find violations.
    SignatureMatchLevel matchLevel = SignatureMatchLevel.STRICT_INVOCATION;

    // Check all parameters other than the last one. The last one will be checked against variable arity.
    for (int i = 0; i < parameterTypes.size() - 1; i++) {
      Optional<SolvedType> solvedParameterType =
          typeSolver.solve(
              parameterTypes.get(i), module, method.getChildScope().getParentScope().get());
      switch (matchArgumentType(argumentTypes.get(i), solvedParameterType, method, module)) {
        case NOT_MATCH:
          return SignatureMatchLevel.TYPE_NOT_MATCH;
        case MATCH_WITH_BOXING:
          matchLevel = SignatureMatchLevel.LOOSE_INVOCATION;
          break;
        case MATCH_WITHOUT_BOXING:
          // The types match without boxing, keep the current match level.
          break;
      }
    }

    if (argumentTypes.size() < parameterTypes.size()) {
      // Passing foo(arg1, arg2) to foo(param1, param2, param3...)
      return SignatureMatchLevel.VARIABLE_ARITY_LOOSE_INVOCATION;
    }

    // Check the last parameter without variable arity considered.
    Optional<SolvedType> lastParameterType =
        typeSolver.solve(
            parameterTypes.get(parameterTypes.size() - 1),
            module,
            method.getChildScope().getParentScope().get());
    if (!lastParameterType.isPresent()) {
      return SignatureMatchLevel.TYPE_NOT_MATCH;
    }
    TypeMatchLevel lastParameterMatchLevel =
        matchArgumentType(
            argumentTypes.get(parameterTypes.size() - 1), lastParameterType, method, module);
    if (lastParameterMatchLevel == TypeMatchLevel.MATCH_WITH_BOXING) {
      matchLevel = SignatureMatchLevel.LOOSE_INVOCATION;
    }
    // If the last parameter matches without variable arity, we are good to return. Otherwise, give
    // it another chance with variable arity considered.
    if (lastParameterMatchLevel != TypeMatchLevel.NOT_MATCH
        && argumentTypes.size() == parameterTypes.size()) {
      return matchLevel;
    }

    if (!isVariableArityInvocation) {
      // Not a variable arity method. There's no way the parameter can match the argument.
      return SignatureMatchLevel.TYPE_NOT_MATCH;
    }

    // Check all of the rest arguments with the de-arrayed last parameter.
    Optional<SolvedType> variableVarityParameter =
        lastParameterType.map((t) -> t.toBuilder().setArray(false).build());
    // Variable arity, check if any type mismatch with the last parameter.
    for (int i = parameterTypes.size(); i < argumentTypes.size(); i++) {
      if (matchArgumentType(argumentTypes.get(i), variableVarityParameter, method, module)
          == TypeMatchLevel.NOT_MATCH) {
        return SignatureMatchLevel.TYPE_NOT_MATCH;
      }
    }
    return SignatureMatchLevel.VARIABLE_ARITY_LOOSE_INVOCATION;
  }

  /**
   * @param argumentType the type of the argument passed to the method
   * @param parameterType the type of the parameter definition of the method
   * @return how does the parameter type match the argument type
   */
  private TypeMatchLevel matchArgumentType(
      Optional<SolvedType> argumentType,
      Optional<SolvedType> parameterType,
      MethodEntity method,
      Module module) {
    if (!argumentType.isPresent()) {
      // Unknown type or untyped lambda, consider as a match since it's the same to all method overloads.
      return TypeMatchLevel.MATCH_WITHOUT_BOXING;
    }
    if (!parameterType.isPresent()) {
      // Unable to solve parameter type. Consider it the lowest match level.
      return TypeMatchLevel.NOT_MATCH;
    }

    // Object can match everything.
    if (OBJECT_FULL_NAME.equals(parameterType.get().getEntity().getQualifiedName())) {
      if (argumentType.get().isPrimitive()) {
        return TypeMatchLevel.MATCH_WITH_BOXING;
      } else {
        return TypeMatchLevel.MATCH_WITHOUT_BOXING;
      }
    }

    if (typeMatchWithoutBoxing(argumentType.get(), parameterType.get(), module)) {
      return TypeMatchLevel.MATCH_WITHOUT_BOXING;
    } else if (typeMatchWithAutoBoxing(argumentType.get(), parameterType.get())) {
      return TypeMatchLevel.MATCH_WITH_BOXING;
    } else {
      return TypeMatchLevel.NOT_MATCH;
    }
  }

  /** @return {@code true} if argumentType can be assigned to parameterTypes without auto-boxing */
  private boolean typeMatchWithoutBoxing(
      SolvedType argumentType, SolvedType parameterType, Module module) {
    if (argumentType.isArray() != parameterType.isArray()
        || argumentType.isPrimitive() != parameterType.isPrimitive()) {
      return false;
    }

    if (argumentType.isPrimitive()) {
      if (argumentType.isArray()) {
        // No type coercion for arrays.
        return argumentType.getEntity().equals(parameterType.getEntity());
      }
      return primitiveTypeMatch(
          (PrimitiveEntity) argumentType.getEntity(), (PrimitiveEntity) parameterType.getEntity());
    }

    if (!(argumentType.getEntity() instanceof ClassEntity
        && parameterType.getEntity() instanceof ClassEntity)) {
      // You wrote wrong code to pass a package as an argument.
      return false;
    }

    for (ClassEntity argumentBaseClass :
        typeSolver.classHierarchy((ClassEntity) argumentType.getEntity(), module)) {
      if (argumentBaseClass
          .getQualifiedName()
          .equals(parameterType.getEntity().getQualifiedName())) {
        // TODO: consider type parameters.
        return true;
      }
    }
    return false;
  }

  /** Checks if argumentType can be assigned to parameterType with auto-boxing. */
  private static boolean typeMatchWithAutoBoxing(
      SolvedType argumentType, SolvedType parameterType) {
    if (argumentType.isArray() || parameterType.isArray()) {
      // Arrays cannot be applied to auto-boxing.
      return false;
    }
    String primitiveArgumentType = primitiveTypeOf(argumentType);
    if (primitiveArgumentType == null) {
      // Argument type is neither primitive nor boxed primitive type.
      return false;
    }
    String primitiveParameterType = primitiveTypeOf(parameterType);
    return primitiveArgumentType.equals(primitiveParameterType);
  }

  /**
   * Try to get the primitive type of given solvedType.
   *
   * @return the name of the primitive type, or {@code null} if cannot be unboxed to primitive type
   */
  @Nullable
  private static String primitiveTypeOf(SolvedType solvedType) {
    if (solvedType.isArray()) {
      return null;
    }
    if (solvedType.isPrimitive()) {
      return solvedType.getEntity().getSimpleName();
    }
    return UNBOXING_MAP.get(solvedType.getEntity().getQualifiedName());
  }

  /**
   * Checks if the primitive argumentType can be assigned to parameterType.
   *
   * <p>artumentType can be assigned to parameterType if they are the same type, or argumentType can
   * be widening converted to parameterType.
   */
  private static boolean primitiveTypeMatch(
      PrimitiveEntity argumentType, PrimitiveEntity parameterType) {
    if (argumentType.equals(parameterType)) {
      return true;
    }
    return WIDENING_PRIMITIVE_CONVERSION_MAP
        .get(argumentType.getSimpleName())
        .contains(parameterType.getSimpleName());
  }

  /**
   * Gets the relationship between two {@link SignatureMatchLevel} values.
   *
   * @return one of -1, 0, and 1. -1 means overloads with match level of {@code lhs} is more
   *     preferred over those of {@code rhs}. 1 means overloads with match level of {@code rhs} is
   *     more preferred over thos of {@code lhs}. 0 means {@code rhs} and {@code lhs} are identical
   */
  private static int compareMatchLevel(SignatureMatchLevel rhs, SignatureMatchLevel lhs) {
    int rordinal = rhs.ordinal();
    int lordinal = lhs.ordinal();
    if (rordinal < lordinal) {
      return -1;
    } else if (rordinal > lordinal) {
      return 1;
    } else {
      return 0;
    }
  }

  private MethodEntity getMostSpecificMethod(
      List<MethodEntity> methods,
      int numArguments,
      SignatureMatchLevel signatureMatchLevel,
      Module module) {
    Set<MethodEntity> lessSpecificMethods = new HashSet<>();
    for (int i = 0; i < methods.size(); i++) {
      MethodEntity method1 = methods.get(i);
      if (lessSpecificMethods.contains(method1)) {
        continue;
      }
      innerLoop:
      for (int j = i + 1; j < methods.size(); j++) {
        MethodEntity method2 = methods.get(j);
        if (lessSpecificMethods.contains(method2)) {
          continue;
        }
        int compareResult =
            compareMethodSpecificity(method1, method2, numArguments, signatureMatchLevel, module);
        switch (compareResult) {
          case -1:
            // method 1 is less specific than method 2.
            lessSpecificMethods.add(method1);
            break innerLoop;
          case 1:
            // method 1 is more specific than method 2.
            lessSpecificMethods.add(method2);
            break;
          case 0:
            // Neither is more specific than the other, no-op.
            break;
        }
      }
    }
    // Pick the first one that is not less specific
    for (MethodEntity method : methods) {
      if (!lessSpecificMethods.contains(method)) {
        return method;
      }
    }

    // Shouldn't reach here.
    logger.warning("No most specific method picked.");
    return null;
  }

  /**
   * Checks which method is more specific than the other.
   *
   * @return -1 if {@code lhs} is less specific than {@code rhs}, 1 if {@code lhs} is more specific
   *     than {@code rhs}, 0 otherwise.
   */
  private int compareMethodSpecificity(
      MethodEntity lhs,
      MethodEntity rhs,
      int numArguments,
      SignatureMatchLevel signatureMatchLevel,
      Module module) {
    List<Optional<SolvedType>> lhsParameterTypes =
        lhs.getParameters()
            .stream()
            .map(
                p ->
                    typeSolver.solve(
                        p.getType(), module, lhs.getChildScope().getParentScope().get()))
            .collect(Collectors.toList());
    List<Optional<SolvedType>> rhsParameterTypes =
        rhs.getParameters()
            .stream()
            .map(
                p ->
                    typeSolver.solve(
                        p.getType(), module, rhs.getChildScope().getParentScope().get()))
            .collect(Collectors.toList());
    if (methodMoreSpecific(
        lhs,
        lhsParameterTypes,
        rhs,
        rhsParameterTypes,
        numArguments,
        signatureMatchLevel,
        module)) {
      return 1;
    }
    if (methodMoreSpecific(
        rhs,
        rhsParameterTypes,
        lhs,
        lhsParameterTypes,
        numArguments,
        signatureMatchLevel,
        module)) {
      return -1;
    }
    return 0;
  }

  /** Moves the best matched method to the first element. */
  public List<Entity> prioritizeMatchedMethod(
      List<Entity> entities, List<Optional<SolvedType>> argumentTypes, Module module) {
    if (entities.isEmpty()) {
      return entities;
    }
    MethodEntity matchedMethod = solve(entities, argumentTypes, module);

    ImmutableList.Builder<Entity> builder = new ImmutableList.Builder<>();
    builder.add(matchedMethod);

    for (Entity entity : entities) {
      if (entity != matchedMethod) {
        builder.add(entity);
      }
    }
    return builder.build();
  }

  /** Determines if {@code lhs} method is more specific than {@code rhs} method. */
  private boolean methodMoreSpecific(
      MethodEntity lhs,
      List<Optional<SolvedType>> lhsParameterTypes,
      MethodEntity rhs,
      List<Optional<SolvedType>> rhsParameterTypes,
      int numArguments,
      SignatureMatchLevel signatureMatchLevel,
      Module module) {
    int maxParameterSize =
        Math.max(Math.max(lhsParameterTypes.size(), rhsParameterTypes.size()), numArguments);
    boolean isVariableArityInvocation =
        (signatureMatchLevel == SignatureMatchLevel.VARIABLE_ARITY_LOOSE_INVOCATION);
    for (int i = 0; i < maxParameterSize; i++) {
      for (Optional<SolvedType> lhsParameterType :
          getParameterTypesAtIndex(lhsParameterTypes, i, isVariableArityInvocation)) {
        for (Optional<SolvedType> rhsParameterType :
            getParameterTypesAtIndex(rhsParameterTypes, i, isVariableArityInvocation)) {
          if (matchArgumentType(lhsParameterType, rhsParameterType, lhs, module)
              == TypeMatchLevel.NOT_MATCH) {
            return false;
          }
        }
      }
    }
    return true;
  }

  /**
   * Returns all possible parameter types for the index-th parameter.
   *
   * <p>If the index-th parameter is before the last formal parameter, return the type itself.
   *
   * <p>If the index-th parameter is the last formal parameter or after the last formal parameter.
   * Then it depends on whether the method invocation is a variable-arity invocation. If the method
   * invocation is a variable-arity invocation, then return the base type of the array of the last
   * formal parameter. Otherwise:
   *
   * <ol>
   *   <li>If the index-th parameter is the last formal parameter, then return the type itself.
   *   <li>If the index-th parameter is after the last formal parameter, then return nothing.
   * </ol>
   */
  private List<Optional<SolvedType>> getParameterTypesAtIndex(
      List<Optional<SolvedType>> formalParameterTypes,
      int index,
      boolean isVariableArityInvocation) {
    checkArgument(index >= 0);
    if (index < formalParameterTypes.size() - 1) {
      return ImmutableList.of(formalParameterTypes.get(index));
    }

    Optional<SolvedType> lastFormalParameterType =
        formalParameterTypes.get(formalParameterTypes.size() - 1);

    if (!isVariableArityInvocation) {
      if (index == formalParameterTypes.size() - 1) {
        return ImmutableList.of(lastFormalParameterType);
      } else {
        return ImmutableList.of();
      }
    }

    // index >= formalParameterTypes.size() and isVariableArityInvocation
    checkState(
        lastFormalParameterType.isPresent() && lastFormalParameterType.get().isArray(),
        "Variable arity invocation with unknown type or non-array last parameter");
    Optional<SolvedType> variableArityType =
        Optional.of(lastFormalParameterType.get().toBuilder().setArray(false).build());
    return ImmutableList.of(variableArityType);
  }
}
