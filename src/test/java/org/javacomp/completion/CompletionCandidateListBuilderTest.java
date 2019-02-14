package org.javacomp.completion;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class CompletionCandidateListBuilderTest {

  @Test
  public void filterAndSortCandidatesByPrefix() {
    String prefix = "fooB";
    CompletionCandidateListBuilder builder = new CompletionCandidateListBuilder(prefix);
    builder
        .addCandidate(createSimpleCandidate("foobar"))
        .addCandidate(createSimpleCandidate("Foobar"))
        .addCandidate(createSimpleCandidate("fooBar"))
        .addCandidate(createSimpleCandidate("FooBar"))
        .addCandidate(createSimpleCandidate("foo"))
        .addCandidate(createSimpleCandidate("fooB"))
        .addCandidate(createSimpleCandidate("Foob"))
        .addCandidate(createSimpleCandidate("foob"));
    assertThat(buildCandidateNameList(builder))
        .containsExactly("fooB", "Foob", "foob", "fooBar", "FooBar", "Foobar", "foobar")
        .inOrder();
  }

  private SimpleCompletionCandidate createSimpleCandidate(String name) {
    return SimpleCompletionCandidate.builder()
        .setName(name)
        .setKind(CompletionCandidate.Kind.UNKNOWN)
        .build();
  }

  private List<String> buildCandidateNameList(CompletionCandidateListBuilder builder) {
    return builder.build().stream()
        .map(candidate -> candidate.getName())
        .collect(ImmutableList.toImmutableList());
  }
}
