package org.javacomp.tool;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ErroneousTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.ModifiersTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.TypeParameterTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.javacomp.parser.SourceFileObject;

/**
 * Print AST tree produced by javac parser.
 *
 * <p>Usage:
 *
 * <pre>
 * bazel run //src/main/java/org/javacomp/tool:AstPrinter -- <java-file-name>
 * </pre>
 */
public class AstPrinter extends TreeScanner<Void, Void> {
  private final String filename;
  private final Context javacContext;
  private final JavacFileManager fileManager;

  private int indent;

  public AstPrinter(String filename) {
    this.filename = filename;
    this.indent = 0;
    this.javacContext = new Context();
    this.fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
  }

  public void scan() {
    try {
      String input = new String(Files.readAllBytes(Paths.get(filename)), UTF_8);

      // Set source file of the log before parsing. If not set, IllegalArgumentException will be
      // thrown if the parser enconters errors.
      SourceFileObject sourceFileObject = new SourceFileObject(filename);
      Log javacLog = Log.instance(javacContext);
      javacLog.useSource(sourceFileObject);

      // Create a parser and start parsing.
      JavacParser parser =
          ParserFactory.instance(javacContext)
              .newParser(
                  input, true /* keepDocComments */, true /* keepEndPos */, true /* keepLineMap */);
      scan(parser.parseCompilationUnit(), null);
      System.out.println("");
    } catch (IOException e) {
      System.exit(1);
    }
  }

  @Override
  public Void scan(Tree node, Void unused) {
    if (node == null) {
      System.out.print(" <null>");
      return null;
    }
    printIndent();
    System.out.print(node.getClass().getSimpleName());
    indent += 2;
    super.scan(node, null);
    indent -= 2;
    return null;
  }

  @Override
  public Void visitMemberSelect(MemberSelectTree node, Void unused) {
    System.out.print(" " + node.getIdentifier().toString());
    super.visitMemberSelect(node, unused);
    return null;
  }

  @Override
  public Void visitIdentifier(IdentifierTree node, Void unused) {
    System.out.print(" " + node.getName());
    return null;
  }

  @Override
  public Void visitModifiers(ModifiersTree node, Void unused) {
    System.out.print(" " + Joiner.on(", ").join(node.getFlags()));
    super.visitModifiers(node, null);
    return null;
  }

  @Override
  public Void visitErroneous(ErroneousTree node, Void unused) {
    scan(node.getErrorTrees(), null);
    return null;
  }

  @Override
  public Void visitClass(ClassTree node, Void unused) {
    System.out.print(" " + node.getSimpleName());
    scan(node.getModifiers(), null);
    printWithIndent("[type parameters]:");
    scan(node.getTypeParameters(), null);
    printWithIndent("[extend clause]:");
    scan(node.getExtendsClause(), null);
    printWithIndent("[implements clause]:");
    scan(node.getImplementsClause(), null);
    printWithIndent("[members]:");
    scan(node.getMembers(), null);
    return null;
  }

  @Override
  public Void visitTypeParameter(TypeParameterTree node, Void unused) {
    System.out.print(" " + node.getName());
    printWithIndent("[annotations]");
    scan(node.getAnnotations(), null);
    printWithIndent("[bounds]");
    scan(node.getBounds(), null);
    return null;
  }

  @Override
  public Void visitMethod(MethodTree node, Void unused) {
    System.out.print(" " + node.getName());
    printWithIndent("[modifiers]");
    scan(node.getModifiers(), null);
    printWithIndent("[return type]");
    scan(node.getReturnType(), null);
    printWithIndent("[type parameters]");
    scan(node.getTypeParameters(), null);
    printWithIndent("[parameters]");
    scan(node.getParameters(), null);
    printWithIndent("[receiver parameter]");
    scan(node.getReceiverParameter(), null);
    printWithIndent("[throws]");
    scan(node.getThrows(), null);
    printWithIndent("[body]");
    scan(node.getBody(), null);
    printWithIndent("[default value]");
    scan(node.getDefaultValue(), null);
    return null;
  }

  @Override
  public Void visitVariable(VariableTree node, Void unused) {
    System.out.print(" " + node.getName());
    printWithIndent("[modifiers]");
    scan(node.getModifiers(), null);
    printWithIndent("[type]");
    scan(node.getType(), null);
    printWithIndent("[name expression]");
    scan(node.getNameExpression(), null);
    printWithIndent("[initializer]");
    scan(node.getInitializer(), null);
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree node, Void unused) {
    scan(node.getMethodSelect(), null);
    printWithIndent("[arguments]");
    scan(node.getArguments(), null);
    printWithIndent("[type arguments]");
    scan(node.getTypeArguments(), null);
    return null;
  }

  private void printWithIndent(String suffix) {
    printIndent();
    System.out.print(suffix);
  }

  private void printIndent() {
    System.out.println("");
    for (int i = 0; i < indent; i++) {
      System.out.print(" ");
    }
  }

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Usage: AstPrinter <javafile>");
      System.exit(1);
    }
    new AstPrinter(args[0]).scan();
  }
}
