package org.javacomp.model;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;

public interface EntityScope {
  Multimap<String, Entity> getMemberEntities();

  Optional<EntityScope> getParentScope();

  List<EntityScope> getChildScopes();

  /** Returns the entity that defines this scope. */
  Optional<Entity> getDefiningEntity();

  void addEntity(Entity entity);

  void addChildScope(EntityScope entityScope);
}
