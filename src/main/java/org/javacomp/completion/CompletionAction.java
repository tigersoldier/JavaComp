package org.javacomp.completion;

import com.google.common.collect.Multimap;
import org.javacomp.model.GlobalIndex;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityIndex;

/** Action to perform the requested completion. */
interface CompletionAction {
  public Multimap<String, Entity> getVisibleEntities(
      GlobalIndex globalIndex, EntityIndex completionPointIndex);
}
