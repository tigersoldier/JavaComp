package org.javacomp.model;

import com.google.common.collect.Multimap;
import java.util.Optional;

public interface EntityScope {
  Multimap<String, Entity> getMemberEntities();

  Optional<EntityScope> getParentScope();

  void addEntity(Entity entity);
}
