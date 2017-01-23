package org.javacomp.completion;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityIndex;

/** An action that returns any visible entities as completion candidates. */
class CompleteEntityAction implements CompletionAction {
  @Override
  public Multimap<String, Entity> getVisibleEntities(
      GlobalIndex globalIndex, EntityIndex completionPointIndex) {
    ImmutableMultimap.Builder<String, Entity> entityMapBuilder = new ImmutableMultimap.Builder<>();
    entityMapBuilder.putAll(globalIndex.getAllEntities());
    entityMapBuilder.putAll(completionPointIndex.getAllEntities());
    return entityMapBuilder.build();
  }
}
