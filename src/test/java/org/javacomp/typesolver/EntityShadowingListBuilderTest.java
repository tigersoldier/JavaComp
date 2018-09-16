package org.javacomp.typesolver;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.common.truth.IterableSubject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class EntityShadowingListBuilderTest {

  private FileScope fileScope;
  private ClassEntity classEntity;
  private ClassEntity classEntity2;
  private MethodEntity methodEntity;
  private VariableEntity variableEntity;
  private VariableEntity variableEntity2;

  @Before
  public void setUpEntities() {
    fileScope = FileScope.createForTesting(ImmutableList.of());
    classEntity =
        new ClassEntity(
            "foo",
            Entity.Kind.CLASS,
            ImmutableList.of() /* qualifiers */,
            true /* isStatic */,
            fileScope,
            Optional.empty() /* superClass */,
            ImmutableList.of() /* interfaces */,
            ImmutableList.of() /* typeParameters */,
            Range.all() /* classNameRange */,
            Range.all() /* definitionRange */);
    classEntity2 =
        new ClassEntity(
            "foo",
            Entity.Kind.CLASS,
            ImmutableList.of() /* qualifiers */,
            true /* isStatic */,
            fileScope,
            Optional.empty() /* superClass */,
            ImmutableList.of() /* interfaces */,
            ImmutableList.of() /* typeParameters */,
            Range.all() /* classNameRange */,
            Range.all() /* definitionRange */);
    variableEntity =
        new VariableEntity(
            "foo",
            Entity.Kind.VARIABLE,
            ImmutableList.of() /* qualifiers */,
            false /* isStatic */,
            TypeReference.JAVA_LANG_OBJECT,
            classEntity,
            Range.all(), /* variableNameRange */
            Range.all() /* definitionRange */);
    variableEntity2 =
        new VariableEntity(
            "foo",
            Entity.Kind.FIELD,
            ImmutableList.of() /* qualifiers */,
            false /* isStatic */,
            TypeReference.JAVA_LANG_OBJECT,
            classEntity,
            Range.all(), /* variableNameRange */
            Range.all() /* definitionRange */);
    methodEntity =
        new MethodEntity(
            "foo",
            ImmutableList.of() /* qualifiers */,
            false /* isStatic */,
            TypeReference.JAVA_LANG_OBJECT /* returnType */,
            ImmutableList.of() /* parameters */,
            ImmutableList.of() /* typeParameters */,
            classEntity,
            Range.all(), /* methodNameRange */
            Range.all() /* definitionRange */);
  }

  @Test
  public void nonMethodShadowsNonMethod() {
    assertShadows(classEntity, variableEntity);
    assertShadows(classEntity, classEntity2);
    assertShadows(variableEntity, classEntity);
    assertShadows(variableEntity, variableEntity2);
  }

  @Test
  public void nonMethodDoesNotShadowMethod() {
    assertNotShadows(variableEntity, methodEntity);
    assertNotShadows(classEntity, methodEntity);
  }

  @Test
  public void methodDoesNotShadowNonMethod() {
    assertNotShadows(methodEntity, variableEntity);
    assertNotShadows(methodEntity, classEntity);
  }

  @Test
  public void nullDoesNotShadowAnything() {
    assertNotShadows(null, variableEntity);
    assertNotShadows(null, classEntity);
    assertNotShadows(null, methodEntity);
    assertNotShadows(null, null);
  }

  @Test
  public void nothingShadowsNull() {
    assertNotShadows(variableEntity, null);
    assertNotShadows(classEntity, null);
    assertNotShadows(methodEntity, null);
  }

  @Test
  public void secondEntityShadowsThird() {
    assertList(buildList(methodEntity, classEntity, variableEntity))
        .containsExactly(methodEntity, classEntity)
        .inOrder();
  }

  private void assertShadows(Entity entity1, Entity entity2) {
    assertList(buildList(entity1, entity2)).containsExactly(entity1);
  }

  private void assertNotShadows(@Nullable Entity entity1, @Nullable Entity entity2) {
    assertList(buildList(entity1, entity2)).containsExactly(entity1, entity2).inOrder();
  }

  private List<TestEntityContainer> buildList(@Nullable Entity... entities) {
    EntityShadowingListBuilder<TestEntityContainer> builder = createBuilder();
    for (Entity entity : entities) {
      TestEntityContainer container = new TestEntityContainer(entity);
      builder.add(container);
    }
    return builder.build();
  }

  private static EntityShadowingListBuilder<TestEntityContainer> createBuilder() {
    return new EntityShadowingListBuilder<>(TestEntityContainer::getEntity);
  }

  private IterableSubject assertList(List<TestEntityContainer> list) {
    return assertThat(
        list.stream().map(TestEntityContainer::getEntity).collect(Collectors.toList()));
  }

  private static class TestEntityContainer {
    @Nullable private final Entity entity;

    private TestEntityContainer(Entity entity) {
      this.entity = entity;
    }

    private TestEntityContainer() {
      this.entity = null;
    }

    @Nullable
    private Entity getEntity() {
      return entity;
    }
  }
}
