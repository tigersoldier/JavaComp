package org.javacomp.typesolver;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.javacomp.model.Entity;

/**
 * A build of {@link List} that ignores a new element the list if any {@link Entity} from existing
 * elements can shadow the {@link Entity} from the new element.
 *
 * <p>An {@link Entity} a can shadow another {@link Entity} b if:
 *
 * <ul>
 *   <li>Neither a nor b is a method; or
 *   <li>Both a and b are methods, and a can override b.
 * </ul>
 */
public class EntityShadowingListBuilder<E> {
  private final ArrayList<E> elements;
  private final Function<E, Entity> getEntityFunction;

  /**
   * @param getEntityFunction a function to get an {@link Entity} from an element for shadowing
   *     check. If the function returns {@code null}, the element won't shadow any other element and
   *     won't be shadowed
   */
  public EntityShadowingListBuilder(Function<E, Entity> getEntityFunction) {
    this.elements = new ArrayList<>();
    this.getEntityFunction = getEntityFunction;
  }

  public EntityShadowingListBuilder<E> add(E newElement) {
    Entity newEntity = getEntityFunction.apply(newElement);
    for (E existing : elements) {
      if (entityShadows(getEntityFunction.apply(existing), newEntity)) {
        return this;
      }
    }
    elements.add(newElement);
    return this;
  }

  public ImmutableList<E> build() {
    return ImmutableList.copyOf(elements);
  }

  public Stream<E> stream() {
    return elements.stream();
  }

  private boolean entityShadows(@Nullable Entity existingEntity, @Nullable Entity newEntity) {
    if (existingEntity == null || newEntity == null) {
      return false;
    }
    boolean existingIsMethod = existingEntity.getKind() == Entity.Kind.METHOD;
    boolean newIsMethod = newEntity.getKind() == Entity.Kind.METHOD;
    if (existingIsMethod != newIsMethod) {
      return false;
    }
    if (!existingIsMethod) {
      return true;
    }

    // TODO: Implement method overriding detection.
    return false;
  }
}
