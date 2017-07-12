package org.javacomp.completion;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.javacomp.model.Entity;
import org.javacomp.typesolver.EntityShadowingListBuilder;

/**
 * A builder for bulding a {@link List} of {@link CompletionCandidate} instances that dedups the
 * candidates with the same name using {@link EntityShadowingListBuilder}.
 */
public class CompletionCandidateListBuilder {
  private static final GetElementFunction GET_ELEMENT_FUNCTION = new GetElementFunction();

  private final Map<String, EntityShadowingListBuilder<CompletionCandidate>> candidateMap;

  public CompletionCandidateListBuilder() {
    candidateMap = new HashMap<>();
  }

  public boolean hasCandidateWithName(String name) {
    return candidateMap.containsKey(name);
  }

  public CompletionCandidateListBuilder addEntities(Multimap<String, Entity> entities) {
    for (Entity entity : entities.values()) {
      addEntity(entity);
    }
    return this;
  }

  public CompletionCandidateListBuilder addEntities(Collection<CompletionCandidate> entities) {
    for (CompletionCandidate entity : entities) {
      addCandidate(entity);
    }
    return this;
  }

  public CompletionCandidateListBuilder addEntity(Entity entity) {
    return this.addCandidate(new EntityCompletionCandidate(entity));
  }

  public CompletionCandidateListBuilder addCandidate(CompletionCandidate candidate) {
    String name = candidate.getName();
    if (!candidateMap.containsKey(name)) {
      candidateMap.put(name, new EntityShadowingListBuilder<>(GET_ELEMENT_FUNCTION));
    }
    candidateMap.get(name).add(candidate);
    return this;
  }

  public ImmutableList<CompletionCandidate> build() {
    ImmutableList.Builder<CompletionCandidate> builder = new ImmutableList.Builder<>();
    for (EntityShadowingListBuilder<CompletionCandidate> candidatesBuilder :
        candidateMap.values()) {
      builder.addAll(candidatesBuilder.build());
    }
    return builder.build();
  }

  private static class GetElementFunction implements Function<CompletionCandidate, Entity> {
    @Override
    public Entity apply(CompletionCandidate candidate) {
      if (candidate instanceof EntityCompletionCandidate) {
        return ((EntityCompletionCandidate) candidate).getEntity();
      } else {
        return null;
      }
    }
  }
}
