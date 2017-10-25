package org.javacomp.completion;

import com.google.common.base.Joiner;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.LineMap;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.EndPosTable;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.protocol.Position;
import org.javacomp.protocol.Range;
import org.javacomp.protocol.TextEdit;

/** Generates text edits for completion items. */
public class TextEdits {
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final long INVALID_POS = -1;

  public Optional<TextEdit> forImportClass(Module module, Path filePath, String fullClassName) {
    Optional<FileScope> fileScope = module.getFileScope(filePath.toString());

    if (!fileScope.isPresent() || !fileScope.get().getCompilationUnit().isPresent()) {
      return Optional.empty();
    }

    JCCompilationUnit complationUnit = fileScope.get().getCompilationUnit().get();
    LineMap lineMap = fileScope.get().getLineMap().get();
    EndPosTable endPosTable = complationUnit.endPositions;

    ImportClassScanner scanner = new ImportClassScanner(fullClassName, lineMap, endPosTable);
    scanner.scan(complationUnit, null);

    int numNewLineBefore = 0;
    int numNewLineAfter = 0;
    TextEdit textEdit = new TextEdit();

    if (scanner.afterImportPos != INVALID_POS) {
      // New line, then insert import statement;
      textEdit.range = createRange(scanner.afterImportPos, lineMap);
      numNewLineBefore = 1;
    } else if (scanner.beforeImportPos != INVALID_POS) {
      // Insert import statement, then new line.
      textEdit.range = createRange(scanner.beforeImportPos, lineMap);
      numNewLineAfter = 1;
    } else if (scanner.afterStaticImportsPos != INVALID_POS) {
      // 1 blank line between static import and the new import, so two new lines.
      textEdit.range = createRange(scanner.afterStaticImportsPos, lineMap);
      numNewLineBefore = 2;
    } else if (scanner.afterPackagePos != INVALID_POS) {
      // 1 blank line between package statement and the new import, so two new lines.
      textEdit.range = createRange(scanner.afterPackagePos, lineMap);
      numNewLineBefore = 2;
    } else {
      // No package or import statements, insert to the first line and add a blank line below.
      textEdit.range = new Range(new Position(0, 0), new Position(0, 0));
      numNewLineAfter = 2;
    }

    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < numNewLineBefore; i++) {
      sb.append('\n');
    }
    sb.append("import ").append(fullClassName).append(';');
    for (int i = 0; i < numNewLineAfter; i++) {
      sb.append('\n');
    }
    textEdit.newText = sb.toString();

    return Optional.of(textEdit);
  }

  private static Range createRange(long pos, LineMap lineMap) {
    Position position =
        new Position((int) lineMap.getLineNumber(pos) - 1, (int) lineMap.getColumnNumber(pos) - 1);
    return new Range(position, position);
  }

  private static class ImportClassScanner extends TreeScanner<Void, Void> {
    private final String fullClassName;
    private final LineMap lineMap;
    private final EndPosTable endPosTable;

    private long afterPackagePos = INVALID_POS;
    private long beforeImportPos = INVALID_POS;
    private long afterImportPos = INVALID_POS;
    private long afterStaticImportsPos = INVALID_POS;
    private boolean isImported;

    private ImportClassScanner(String fullClassName, LineMap lineMap, EndPosTable endPosTable) {
      this.fullClassName = fullClassName;
      this.lineMap = lineMap;
      this.endPosTable = endPosTable;
    }

    @Override
    public Void visitCompilationUnit(CompilationUnitTree node, Void unused) {
      if (node.getPackageName() != null) {
        // It's weird that package end pos doesn't contain the ending semicolon.
        afterPackagePos = endPosTable.getEndPos((JCTree) node.getPackageName()) + 1;
      }

      if (node.getImports() != null) {
        for (ImportTree importTree : node.getImports()) {
          scan(importTree, null);
        }
      }
      return null;
    }

    @Override
    public Void visitImport(ImportTree node, Void unused) {
      if (node.isStatic()) {
        afterStaticImportsPos = endPosTable.getEndPos((JCTree) node);
        return null;
      }

      String importedName = nameTreeToQualifiedName(node.getQualifiedIdentifier());
      int cmp = importedName.compareTo(fullClassName);
      if (cmp < 0) {
        // The existing import statement should be above the new one.
        afterImportPos = endPosTable.getEndPos((JCTree) node);
      } else if (cmp > 0) {
        // The new import statement should be above the existing one, and also
        // previous existing ones.
        if (beforeImportPos == INVALID_POS) {
          beforeImportPos = ((JCTree) node).getStartPosition();
        }
      } else {
        isImported = true;
      }

      return null;
    }

    private static String nameTreeToQualifiedName(Tree name) {
      Deque<String> stack = new ArrayDeque<>();
      while (name instanceof MemberSelectTree) {
        MemberSelectTree qualifiedName = (MemberSelectTree) name;
        stack.addFirst(qualifiedName.getIdentifier().toString());
        name = qualifiedName.getExpression();
      }
      stack.addFirst(((IdentifierTree) name).getName().toString());
      return QUALIFIER_JOINER.join(stack);
    }
  }
}
