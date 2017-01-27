package org.javacomp.completion;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;

/** An action to get completion candidates for member selection. */
class CompleteMemberAction implements CompletionAction {
  @Override
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope completionPointScope) {
    // TODO: do type analyzation and return only member entities.
    ImmutableMultimap.Builder<String, Entity> entityMapBuilder = new ImmutableMultimap.Builder<>();
    entityMapBuilder.putAll(globalScope.getAllEntities());
    entityMapBuilder.putAll(completionPointScope.getAllEntities());
    return entityMapBuilder.build();
  }
}
