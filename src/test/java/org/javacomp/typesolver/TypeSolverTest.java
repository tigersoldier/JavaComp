package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.parser.JavacParser;
import com.sun.tools.javac.parser.ParserFactory;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.FileScope;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.TypeReference;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.SourceFileObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TypeSolverTest {
  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/typesolver/testdata/";
  private static final String[] TEST_FILES = {
    "BaseInterface.java",
    "TestClass.java",
    "other/BaseClass.java",
    "other/Shadow.java",
    "ondemand/OnDemand.java",
    "ondemand/Shadow.java",
  };
  private static final String TEST_DATA_PACKAGE = "org.javacomp.typesolver.testdata";
  private static final String TEST_DATA_OTHER_PACKAGE = TEST_DATA_PACKAGE + ".other";
  private static final String TEST_DATA_ONDEMAND_PACKAGE = TEST_DATA_PACKAGE + ".ondemand";
  private static final String TEST_CLASS_FULL_NAME = TEST_DATA_PACKAGE + ".TestClass";
  private static final String TEST_CLASS_FACTORY_FULL_NAME =
      TEST_CLASS_FULL_NAME + ".TestClassFactory";
  private static final String BASE_INTERFACE_FULL_NAME = TEST_DATA_PACKAGE + ".BaseInterface";
  private static final String BASE_INTERFACE_FACTORY_FULL_NAME =
      BASE_INTERFACE_FULL_NAME + ".BaseInterfaceFactory";
  private static final String BASE_CLASS_FULL_NAME = TEST_DATA_OTHER_PACKAGE + ".BaseClass";
  private static final String OTHER_SHADOW_CLASS_FULL_NAME = TEST_DATA_OTHER_PACKAGE + ".Shadow";
  private static final String BASE_INNER_CLASS_FULL_NAME = BASE_CLASS_FULL_NAME + ".BaseInnerClass";
  private static final String ON_DEMAND_CLASS_FULL_NAME = TEST_DATA_ONDEMAND_PACKAGE + ".OnDemand";
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");

  private Log javacLog;
  private GlobalScope globalScope;
  private Context javacContext;
  private final TypeSolver typeSolver = new TypeSolver();

  @Before
  public void setUpTestScope() throws Exception {
    javacContext = new Context();
    javacLog = Log.instance(javacContext);
    JavacFileManager fileManager = new JavacFileManager(javacContext, true /* register */, UTF_8);
    globalScope = new GlobalScope();
    for (String filename : TEST_FILES) {
      String inputFilePath = TEST_DATA_DIR + filename;
      globalScope.addOrReplaceFileScope(parseTestFile(inputFilePath));
    }
  }

  private FileScope parseTestFile(String filePath) throws Exception {
    String fileContent = new String(Files.readAllBytes(Paths.get(filePath)), UTF_8);

    // If source file not set, parser will throw IllegalArgumentException when errors occur.
    SourceFileObject sourceFileObject = new SourceFileObject(filePath);
    javacLog.useSource(sourceFileObject);

    JavacParser parser =
        ParserFactory.instance(javacContext)
            .newParser(
                fileContent,
                true /* keepDocComments */,
                true /* keepEndPos */,
                true /* keepLineMap */);
    JCCompilationUnit compilationUnit = parser.parseCompilationUnit();
    return new AstScanner().startScan(compilationUnit, filePath);
  }

  @Test
  public void solveBaseInterfaceInTheSamePackage() {
    ClassEntity testClass = (ClassEntity) lookupEntity(TEST_CLASS_FULL_NAME);
    TypeReference baseInterfaceReference = testClass.getInterfaces().get(0);
    Optional<SolvedType> solvedType =
        typeSolver.solve(baseInterfaceReference, globalScope, testClass);
    assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getClassEntity()).isSameAs(lookupEntity(BASE_INTERFACE_FULL_NAME));
  }

  @Test
  public void solveClassDefinedInSuperInterface() {
    ClassEntity testClass = (ClassEntity) lookupEntity(TEST_CLASS_FACTORY_FULL_NAME);
    TypeReference baseInterfaceReference = testClass.getInterfaces().get(0);
    Optional<SolvedType> solvedType =
        typeSolver.solve(baseInterfaceReference, globalScope, testClass);
    assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getClassEntity())
        .isSameAs(lookupEntity(BASE_INTERFACE_FACTORY_FULL_NAME));
  }

  @Test
  public void solveInnerClass() {
    SolvedType baseInterface = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".newFactory");
    assertThat(baseInterface.getClassEntity()).isSameAs(lookupEntity(TEST_CLASS_FACTORY_FULL_NAME));
  }

  @Test
  public void solveBaseClassInOtherPackage() {
    ClassEntity testClass = (ClassEntity) lookupEntity(TEST_CLASS_FULL_NAME);
    TypeReference baseClassReference = testClass.getSuperClass().get();
    Optional<SolvedType> solvedType = typeSolver.solve(baseClassReference, globalScope, testClass);
    assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getClassEntity()).isSameAs(lookupEntity(BASE_CLASS_FULL_NAME));
  }

  @Test
  public void solveInnerClassInBaseClassFromOtherPackage() {
    ClassEntity testClass = (ClassEntity) lookupEntity(TEST_CLASS_FACTORY_FULL_NAME);
    TypeReference baseInnerClassReference = testClass.getSuperClass().get();
    Optional<SolvedType> solvedType =
        typeSolver.solve(baseInnerClassReference, globalScope, testClass);
    assertThat(solvedType).isPresent();
    assertThat(solvedType.get().getClassEntity())
        .isSameAs(lookupEntity(BASE_INNER_CLASS_FULL_NAME));
  }

  @Test
  public void solveOnDemandClassImport() {
    SolvedType onDemandClass = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getOnDemand");
    assertThat(onDemandClass.getClassEntity()).isSameAs(lookupEntity(ON_DEMAND_CLASS_FULL_NAME));
  }

  @Test
  public void onDemandPackageClassShouldBeShadowed() {
    // both other and ondemand package define Shadow class. Shadow from other package should be used
    // since it's explicitly imported.
    SolvedType shadowClass = solveMethodReturnType(TEST_CLASS_FULL_NAME + ".getShadow");
    assertThat(shadowClass.getClassEntity()).isSameAs(lookupEntity(OTHER_SHADOW_CLASS_FULL_NAME));
  }

  private SolvedType solveMethodReturnType(String qualifiedMethodName) {
    MethodEntity method = (MethodEntity) lookupEntity(qualifiedMethodName);
    assertThat(method).isNotNull();
    MethodEntity.Overload methodOverload = method.getOverloads().get(0);
    TypeReference methodReturnType = methodOverload.getReturnType();
    Optional<SolvedType> solvedType =
        typeSolver.solve(
            methodReturnType, globalScope, methodOverload.getMethodScope().getParentClass());
    assertThat(solvedType).isPresent();
    return solvedType.get();
  }

  private Entity lookupEntity(String qualifiedName) {
    String[] qualifiers = qualifiedName.split("\\.");
    EntityScope currentScope = globalScope;
    Entity entity = null;
    List<String> currentQualifiers = new ArrayList<>();
    for (String qualifier : qualifiers) {
      currentQualifiers.add(qualifier);
      Collection<Entity> entities = currentScope.getAllEntities().get(qualifier);
      assertWithMessage(QUALIFIER_JOINER.join(currentQualifiers)).that(entities).isNotEmpty();
      entity = Iterables.getFirst(entities, null);
      currentScope = entity.getChildScope();
    }
    return entity;
  }
}
