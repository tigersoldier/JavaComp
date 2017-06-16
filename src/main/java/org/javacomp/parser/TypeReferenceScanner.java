package org.javacomp.parser;

import com.sun.source.tree.ArrayTypeTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.ParameterizedTypeTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.WildcardTree;
import com.sun.source.util.TreeScanner;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeReference;
import org.javacomp.model.WildcardTypeArgument;

public class TypeReferenceScanner extends TreeScanner<Void, Void> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final Deque<String> names = new ArrayDeque<>();
  private final List<TypeArgument> typeArguments = new ArrayList<>();
  private boolean isPrimitive = false;
  private boolean isArray = false;

  public TypeReference getTypeReference(Tree node) {
    names.clear();
    isPrimitive = false;
    isArray = false;
    typeArguments.clear();
    scan(node, null);
    if (names.isEmpty()) {
      // Malformed input, no type can be referenced
      logger.warning("Empty type name with %s", node);
      return TypeReference.EMPTY_TYPE;
    }
    return TypeReference.builder()
        .setFullName(names)
        .setPrimitive(isPrimitive)
        .setArray(isArray)
        .setTypeArguments(typeArguments)
        .build();
  }

  @Override
  public Void visitParameterizedType(ParameterizedTypeTree node, Void unused) {
    scan(node.getType(), unused);
    for (Tree typeArgument : node.getTypeArguments()) {
      if (typeArgument instanceof WildcardTree) {
        typeArguments.add(createWildcardTypeArgument((WildcardTree) typeArgument));
      } else {
        TypeReference typeReference = new TypeReferenceScanner().getTypeReference(typeArgument);
        if (typeReference == TypeReference.EMPTY_TYPE) {
          logger.warning("Unknown type argument: %s", typeArgument);
        }
        typeArguments.add(typeReference);
      }
    }
    return null;
  }

  @Override
  public Void visitArrayType(ArrayTypeTree node, Void unused) {
    isArray = true;
    scan(node.getType(), unused);
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Void unused) {
    names.addFirst(node.getIdentifier().toString());
    scan(node.getExpression(), unused);
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    names.addFirst(node.getName().toString());
    return null;
  }

  @Override
  public Void visitPrimitiveType(PrimitiveTypeTree node, Void unused) {
    isPrimitive = true;
    names.addFirst(node.getPrimitiveTypeKind().name().toLowerCase());
    return null;
  }

  private WildcardTypeArgument createWildcardTypeArgument(WildcardTree node) {
    Optional<WildcardTypeArgument.Bound> bound;
    switch (node.getKind()) {
      case SUPER_WILDCARD:
        bound =
            Optional.of(
                WildcardTypeArgument.Bound.create(
                    WildcardTypeArgument.Bound.Kind.SUPER,
                    new TypeReferenceScanner().getTypeReference(node.getBound())));
        break;
      case EXTENDS_WILDCARD:
        bound =
            Optional.of(
                WildcardTypeArgument.Bound.create(
                    WildcardTypeArgument.Bound.Kind.EXTENDS,
                    new TypeReferenceScanner().getTypeReference(node.getBound())));
        break;
      case UNBOUNDED_WILDCARD:
        bound = Optional.empty();
        break;
      default:
        logger.warning("Unknown wildcard type varialbe kind: %s", node.getKind());
        bound = Optional.empty();
    }
    return WildcardTypeArgument.create(bound);
  }
}
