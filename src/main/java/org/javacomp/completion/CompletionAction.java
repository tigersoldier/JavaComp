package org.javacomp.completion;

import com.google.common.collect.Multimap;
import org.javacomp.model.GlobalScope;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;

/** Action to perform the requested completion. */
interface CompletionAction {
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope completionPointScope);
}
