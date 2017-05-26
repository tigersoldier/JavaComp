package org.javacomp.reference;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import org.javacomp.model.MethodEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SignatureSolverTest extends BaseTest {
  private static final Joiner PARAMETER_JOINER = Joiner.on(", ");

  private final SignatureSolver signatureSolver = new SignatureSolver();

  @Test
  public void testMethodSignatures() {
    assertSignature(TEST_CLASS_FILE, "overloadMethod(innerA, innerAParam.testClassInA)")
        .hasBestMatch(new SignatureMatcher("overloadMethod", "innerClassA", "otherClass"))
        .hasOtherMethods(
            new SignatureMatcher("overloadMethod", "innerClassA"),
            new SignatureMatcher("overloadMethod"))
        .hasActiveParameter("innerA,", 0)
        .hasActiveParameter(" innerAParam.testClassInA", 1);

    assertSignature(TEST_CLASS_FILE, "overloadMethod(getInnerA())")
        .hasBestMatch(new SignatureMatcher("overloadMethod", "innerClassA"))
        .hasOtherMethods(
            new SignatureMatcher("overloadMethod", "innerClassA", "otherClass"),
            new SignatureMatcher("overloadMethod"))
        .hasActiveParameter("getInnerA(", 0);

    assertSignature(TEST_CLASS_FILE, "overloadMethod();")
        .hasBestMatch(new SignatureMatcher("overloadMethod"))
        .hasOtherMethods(
            new SignatureMatcher("overloadMethod", "innerClassA", "otherClass"),
            new SignatureMatcher("overloadMethod", "innerClassA"))
        .hasActiveParameter(")", 0);
  }

  @Test
  public void testEmptySignaturesOnMethodName() {
    assertSignature(TEST_CLASS_FILE, "overloadMethod();").isEmpty("overloadMethod");
  }

  private SignatureAssertion assertSignature(String filename, String methodInvocation) {
    return new SignatureAssertion(filename, methodInvocation);
  }

  private class SignatureAssertion {
    private final String filename;
    private final String methodInvocation;

    private SignatureMatcher bestMatch;
    private SignatureMatcher[] otherMethodsMatcher;

    private SignatureAssertion(String filename, String methodInvocation) {
      this.filename = filename;
      this.methodInvocation = methodInvocation;
    }

    private SignatureAssertion hasBestMatch(SignatureMatcher bestMatch) {
      this.bestMatch = bestMatch;
      return this;
    }

    private SignatureAssertion hasOtherMethods(SignatureMatcher... otherMethodsMatcher) {
      this.otherMethodsMatcher = otherMethodsMatcher;
      return this;
    }

    private SignatureAssertion testBestMatch(MethodSignatures signatures, String debugString) {
      if (bestMatch == null) {
        throw new RuntimeException("hasBestMatch() hasn't been called.");
      }
      assertThat(signatures.getMethods()).named(debugString + " has method").isNotEmpty();
      assertThat(formatMethod(signatures.getMethods().get(0)))
          .named(debugString + " bestMatch")
          .isEqualTo(bestMatch.toString());
      return this;
    }

    private SignatureAssertion testOtherMethods(MethodSignatures signatures, String debugString) {
      assertThat(signatures.getMethods())
          .named("number of methods")
          .hasSize(otherMethodsMatcher.length + 1);
      List<MethodEntity> otherMethods = new ArrayList<>();
      otherMethods.addAll(signatures.getMethods());
      otherMethods.remove(0);

      assertThat(
              otherMethods
                  .stream()
                  .map(m -> formatMethod(m))
                  .collect(ImmutableList.toImmutableList()))
          .named(debugString)
          .containsExactlyElementsIn(
              Arrays.stream(otherMethodsMatcher)
                  .map(m -> m.toString())
                  .collect(ImmutableList.toImmutableList()));
      return this;
    }

    private void forEachSymbolPosition(
        String symbol, BiConsumer<MethodSignatures, String> consumer) {
      SymbolLocator symbolLocator = new SymbolLocator(filename, methodInvocation, symbol);
      symbolLocator.forEachPosition(
          (textPosition, debugString) -> {
            MethodSignatures signatures =
                signatureSolver.getMethodSignatures(
                    module,
                    Paths.get(symbolLocator.filename),
                    textPosition.getLine(),
                    textPosition.getCharacter());

            consumer.accept(signatures, debugString);
          });
    }

    private SignatureAssertion hasActiveParameter(String parameterContext, int activeParameter) {
      forEachSymbolPosition(
          parameterContext,
          (signatures, debugString) -> {
            testBestMatch(signatures, debugString);
            testOtherMethods(signatures, debugString);
            assertThat(signatures.getActiveParameter())
                .named(debugString + " activeParameter")
                .isEqualTo(activeParameter);
          });
      return this;
    }

    private SignatureAssertion isEmpty(String emptySymbol) {
      forEachSymbolPosition(
          emptySymbol,
          (signatures, debugString) -> {
            assertThat(signatures.getActiveParameter()).named("activeParameter").isEqualTo(0);
            assertThat(signatures.getMethods()).isEmpty();
          });
      return this;
    }

    private String formatMethod(MethodEntity method) {
      StringBuilder sb = new StringBuilder();
      sb.append(method.getSimpleName());
      sb.append("(");
      sb.append(
          PARAMETER_JOINER.join(
              method
                  .getParameters()
                  .stream()
                  .map(v -> v.getSimpleName())
                  .collect(ImmutableList.toImmutableList())));
      sb.append(")");
      return sb.toString();
    }
  }

  private static class SignatureMatcher {
    private final String name;
    private final List<String> parameterNames;

    private SignatureMatcher(String name, String... parameterNames) {
      this.name = name;
      this.parameterNames = ImmutableList.copyOf(parameterNames);
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(name);
      sb.append("(");
      sb.append(PARAMETER_JOINER.join(parameterNames));
      sb.append(")");
      return sb.toString();
    }
  }
}
