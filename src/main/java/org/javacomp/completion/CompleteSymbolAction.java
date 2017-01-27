package org.javacomp.completion;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;

/** An action that returns any visible entities as completion candidates. */
class CompleteEntityAction implements CompletionAction {
  @Override
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope completionPointScope) {
    ImmutableMultimap.Builder<String, Entity> entityMapBuilder = new ImmutableMultimap.Builder<>();
    entityMapBuilder.putAll(globalScope.getAllEntities());
    entityMapBuilder.putAll(completionPointScope.getAllEntities());
    return entityMapBuilder.build();
  }
}
