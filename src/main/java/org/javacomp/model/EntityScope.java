package org.javacomp.model;

import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Optional;

public interface EntityScope {
  List<Entity> getEntitiesWithName(String simpleName);

  Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind);

  Multimap<String, Entity> getAllEntities();

  Multimap<String, Entity> getMemberEntities();

  Optional<EntityScope> getParentScope();

  void addEntity(Entity entity);
}
