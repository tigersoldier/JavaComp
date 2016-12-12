package org.javacomp.parser;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import java.util.List;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.lang.model.element.Modifier;
import org.javacomp.model.SymbolIndexScope;
import org.javacomp.proto.SymbolProto.Symbol;

public class AstScanner extends TreeScanner<Void, SymbolIndexScope> {

  @Override
  public Void visitCompilationUnit(CompilationUnitTree node, SymbolIndexScope scope) {
    if (node.getPackageName() != null) {
      List<String> qualifiers = nameToQualifiers(node.getPackageName());
      scope =
        scope.findOrCreatePackageScope(qualifiers);
    }
    for (Tree decl : node.getTypeDecls()) {
      this.scan(decl, scope);
    }
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, SymbolIndexScope indexScope) {
    Symbol.Type symbolType = Symbol.Type.TYPE_UNKNOWN;
    switch (node.getKind()) {
      case CLASS:
        symbolType = Symbol.Type.CLASS;
        break;
      case INTERFACE:
        symbolType = Symbol.Type.INTERFACE;
        break;
      case ENUM:
        symbolType = Symbol.Type.ENUM;
        break;
      case ANNOTATION_TYPE:
        symbolType = Symbol.Type.ANNOTATION;
      default:
        System.out.println("Unknown class type: " + node.getKind());
    }
    Symbol symbol =
        Symbol.newBuilder()
            .setType(symbolType)
            .setSimpleName(node.getSimpleName().toString())
            .addAllQualifier(indexScope.getQualifiers())
            .setAccessLevel(getAccessLevel(node.getModifiers()))
            .setIsStatic(isStatic(node.getModifiers()))
            .build();
    SymbolIndexScope classScope = indexScope.addSymbol(symbol);
    for (Tree member : node.getMembers()) {
      scan(member, classScope);
    }
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, SymbolIndexScope scope) {
    Symbol symbol =
        Symbol.newBuilder()
            .setType(Symbol.Type.METHOD)
            .setSimpleName(node.getName().toString())
            .setAccessLevel(getAccessLevel(node.getModifiers()))
            .setIsStatic(isStatic(node.getModifiers()))
            .build();
    SymbolIndexScope methodScope = scope.addSymbol(symbol);
    if (node.getBody() != null) {
      visitBlock(node.getBody(), methodScope);
    }
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, SymbolIndexScope scope) {
    Symbol symbol =
        Symbol.newBuilder()
            .setType(Symbol.Type.VARIABLE)
            .setSimpleName(node.getName().toString())
            .setAccessLevel(getAccessLevel(node.getModifiers()))
            .setIsStatic(isStatic(node.getModifiers()))
            .build();
    scope.addSymbol(symbol);
    return null;
  }

  private static Symbol.AccessLevel getAccessLevel(ModifiersTree modifiersTree) {
    for (Modifier modifier : modifiersTree.getFlags()) {
      switch (modifier) {
        case PUBLIC:
          return Symbol.AccessLevel.PUBLIC;
        case PROTECTED:
          return Symbol.AccessLevel.PROTECTED;
        case PRIVATE:
          return Symbol.AccessLevel.PRIVATE;
      }
    }
    return Symbol.AccessLevel.PACKAGE_PRIVATE;
  }

  private static boolean isStatic(ModifiersTree modifiersTree) {
    return modifiersTree.getFlags().contains(Modifier.STATIC);
  }

  private static List<String> nameToQualifiers(ExpressionTree name) {
    Deque<String> stack = new ArrayDeque<>();
    while (name instanceof MemberSelectTree) {
      MemberSelectTree qualifiedName = (MemberSelectTree) name;
      stack.addFirst(qualifiedName.getIdentifier().toString());
      name = qualifiedName.getExpression();
    }
    stack.addFirst(((IdentifierTree) name).getName().toString());
    return ImmutableList.copyOf(stack);
  }
}
