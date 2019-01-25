package org.javacomp.completion;

import java.util.Optional;
import org.javacomp.model.Entity;

/** A completion candidate backed by a {@link Entity}. */
abstract class EntityBasedCompletionCandidate implements CompletionCandidate {
  private final Entity entity;

  EntityBasedCompletionCandidate(Entity entity) {
    this.entity = entity;
  }

  Entity getEntity() {
    return entity;
  }

  @Override
  public Optional<String> getJavadoc() {
    return getEntity().getJavadoc();
  }
}
