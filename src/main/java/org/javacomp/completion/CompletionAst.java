package org.javacomp.completion;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;

/** Retrieve completion information from the AST. */
class CompletionAst {
  private final CompletionAstScanner completionAstScanner;

  CompletionAst() {
    completionAstScanner = new CompletionAstScanner();
  }

  public CompletionAction getCompletionAction(JCCompilationUnit compilationUnit) {
    JCTree lastCompletionTree = completionAstScanner.scan(compilationUnit, null);
    if (lastCompletionTree instanceof MemberSelectTree) {
      return new CompleteMemberAction();
    }
    return new CompleteSymbolAction();
  }

  /** An AST scanner that returns the last meaningful sub tree for completion. */
  private static class CompletionAstScanner extends TreeScanner<JCTree, Void> {
    @Override
    public JCTree reduce(JCTree t1, JCTree t2) {
      return t1 != null ? t1 : t2;
    }

    @Override
    public JCTree visitCompilationUnit(CompilationUnitTree node, Void unused) {
      JCTree ret = (JCTree) node;
      for (ImportTree importTree : node.getImports()) {
        ret = (JCTree) importTree;
      }
      for (Tree typeDecl : node.getTypeDecls()) {
        ret = scan(typeDecl, null);
      }
      return ret;
    }

    @Override
    public JCTree visitExpressionStatement(ExpressionStatementTree node, Void unused) {
      return scan(node.getExpression(), null);
    }

    @Override
    public JCTree visitMemberSelect(MemberSelectTree node, Void unused) {
      return (JCTree) node;
    }
  }
}
