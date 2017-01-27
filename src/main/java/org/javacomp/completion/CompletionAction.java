package org.javacomp.completion;

import com.google.common.collect.Multimap;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.GlobalScope;

/** Action to perform the requested completion. */
interface CompletionAction {
  public Multimap<String, Entity> getVisibleEntities(
      GlobalScope globalScope, EntityScope completionPointScope);
}
