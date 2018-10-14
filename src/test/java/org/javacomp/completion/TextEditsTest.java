package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.truth.Truth8;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.javacomp.file.FileSnapshot;
import org.javacomp.project.SimpleModuleManager;
import org.javacomp.protocol.TextEdit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TextEditsTest {

  @Test
  public void importClass_hasImportsAboveAndBelow_insertsInBetween() throws Exception {
    assertImportClass(
        "package test;\n\nimport test.A;\nimport test.C;\n",
        "test.B",
        "package test;\n\nimport test.A;\nimport test.B;\nimport test.C;\n");
  }

  @Test
  public void importClass_hasImportsAboveOnly_insertsBelow() throws Exception {
    assertImportClass(
        "package test;\n\nimport test.A;\nimport test.A.A1;\n",
        "test.B",
        "package test;\n\nimport test.A;\nimport test.A.A1;\nimport test.B;\n");
  }

  @Test
  public void importClass_hasImportsBelowOnly_insertsAbove() throws Exception {
    assertImportClass(
        "package test;\n\nimport test.C;\nimport test.C.C1;\n",
        "test.B",
        "package test;\n\nimport test.B;\nimport test.C;\nimport test.C.C1;\n");
  }

  @Test
  public void importClass_hasUnOrderedImports_insertsBelow() throws Exception {
    assertImportClass(
        "package test;\n\nimport test.C;\nimport test.C.C1;\nimport test.A;\nimport test.A.A1;\n",
        "test.B",
        "package test;\n\nimport test.C;\nimport test.C.C1;\nimport test.A;\nimport test.A.A1;\n"
            + "import test.B;\n");
  }

  @Test
  public void importClass_hasImportsOnTheSameLine_insertsWithBestEffort() throws Exception {
    assertImportClass(
        "package test;\n\nimport test.A;import test.C;",
        "test.B",
        "package test;\n\nimport test.A;\nimport test.B;import test.C;");
  }

  @Test
  public void importClass_hasStaticImportsOnly_insertsBelowWithBlankLine() throws Exception {
    assertImportClass(
        "package test;\n\nimport static test.A.a;\nimport static test.A.b;\n",
        "test.B",
        "package test;\n\nimport static test.A.a;\nimport static test.A.b;\n"
            + "\nimport test.B;\n");
  }

  @Test
  public void importClass_hasPackageOnly_insertsBelowWithBlankLine() throws Exception {
    assertImportClass("package test;\n", "test.B", "package test;\n\nimport test.B;\n");
  }

  @Test
  public void importClass_hasNoPackageOrImport_insertsOnFirstLine() throws Exception {
    assertImportClass("class A {}", "test.B", "import test.B;\n\nclass A {}");
  }

  private void assertImportClass(String original, String importClassName, String expectedResult) {
    SimpleModuleManager moduleManager = new SimpleModuleManager();
    Path dummyPath = Paths.get("Foo.java");
    moduleManager.getFileManager().openFileForSnapshot(dummyPath.toUri(), original);
    moduleManager.addOrUpdateFile(dummyPath, /* fixContentForParsing= */ false);
    TextEdits textEdits = new TextEdits();
    Optional<TextEdit> textEdit =
        textEdits.forImportClass(moduleManager, dummyPath, importClassName);
    Truth8.assertThat(textEdit).isPresent();

    FileSnapshot snapshot = FileSnapshot.createFromContent(original);
    snapshot.applyEdit(
        textEdit.get().range, Optional.empty() /* rangeLength */, textEdit.get().newText);
    assertThat(snapshot.getContent()).isEqualTo(expectedResult);
  }
}
