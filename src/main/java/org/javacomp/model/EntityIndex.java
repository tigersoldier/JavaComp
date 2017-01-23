package org.javacomp.model;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.List;

public interface EntityIndex {
  List<Entity> getEntitiesWithName(String simpleName);

  Optional<Entity> getEntityWithNameAndKind(String simpleName, Entity.Kind entityKind);

  Multimap<String, Entity> getAllEntities();

  Multimap<String, Entity> getMemberEntities();

  Optional<EntityIndex> getParentIndex();

  void addEntity(Entity entity);
}
