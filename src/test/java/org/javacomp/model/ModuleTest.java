package org.javacomp.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class ModuleTest {
  @Rule public MockitoRule mrule = MockitoJUnit.rule();

  @Mock private Entity entity1;
  @Mock private Entity entity2;
  @Mock private Entity entity3;
  @Mock private Entity entity4;
  @Mock private JCCompilationUnit compilationUnit;

  @Before
  public void setUpMocks() {
    when(entity1.getSimpleName()).thenReturn("entity1");
    when(entity2.getSimpleName()).thenReturn("entity2");
    when(entity3.getSimpleName()).thenReturn("entity3");
    when(entity4.getSimpleName()).thenReturn("entity4");
  }

  private Module module = new Module();

  @Test
  public void addFilesShouldCreatePackages() {
    FileScope fileScope1 =
        new FileScope("filename1", ImmutableList.of("foo", "bar"), compilationUnit);
    FileScope fileScope2 =
        new FileScope("filename2", ImmutableList.of("foo", "bar", "baz"), compilationUnit);
    FileScope fileScope3 =
        new FileScope("filename3", ImmutableList.of("foo", "baz"), compilationUnit);
    FileScope fileScope4 = new FileScope("filename4", ImmutableList.of("fxx"), compilationUnit);
    fileScope1.addEntity(entity1);
    fileScope2.addEntity(entity2);
    fileScope3.addEntity(entity3);
    fileScope4.addEntity(entity4);

    module.addOrReplaceFileScope(fileScope1);
    module.addOrReplaceFileScope(fileScope2);
    module.addOrReplaceFileScope(fileScope3);
    module.addOrReplaceFileScope(fileScope4);

    assertThat(module.getRootPackage().getAllEntities().keys()).containsExactly("foo", "fxx");

    PackageScope foo = getPackage(module.getRootPackage(), "foo").getChildScope();
    PackageScope fooBar = getPackage(foo, "bar").getChildScope();
    PackageScope fooBarBaz = getPackage(fooBar, "baz").getChildScope();
    PackageScope fooBaz = getPackage(foo, "baz").getChildScope();
    PackageScope fxx = getPackage(module.getRootPackage(), "fxx").getChildScope();

    assertThat(foo.getAllEntities().keys()).containsExactly("bar", "baz");
    assertThat(fooBar.getAllEntities().keys()).containsExactly("baz", "entity1");
    assertThat(fooBarBaz.getAllEntities().keys()).containsExactly("entity2");
    assertThat(fooBaz.getAllEntities().keys()).containsExactly("entity3");
    assertThat(fxx.getAllEntities().keys()).containsExactly("entity4");
  }

  @Test
  public void replaceFileWithSamePackageShouldNotRemovePackage() {
    FileScope fileScope1 = new FileScope("foobar", ImmutableList.of("foo", "bar"), compilationUnit);
    FileScope fileScope2 = new FileScope("foobar", ImmutableList.of("foo", "bar"), compilationUnit);

    fileScope1.addEntity(entity1);
    fileScope2.addEntity(entity2);

    module.addOrReplaceFileScope(fileScope1);
    module.addOrReplaceFileScope(fileScope2);

    PackageScope foo = getPackage(module.getRootPackage(), "foo").getChildScope();
    PackageScope fooBar = getPackage(foo, "bar").getChildScope();
    assertThat(fooBar.getAllEntities().keys()).containsExactly("entity2");
  }

  @Test
  public void replaceFileWithDifferentPackageShouldNotRemovePackage() {
    FileScope fileScope1 =
        new FileScope("foobar", ImmutableList.of("foo", "bar", "baz"), compilationUnit);
    FileScope fileScope2 = new FileScope("foobar", ImmutableList.of("foo", "bar"), compilationUnit);
    FileScope fileScope3 = new FileScope("foobar", ImmutableList.of("fxx"), compilationUnit);

    fileScope1.addEntity(entity1);
    fileScope2.addEntity(entity2);
    fileScope3.addEntity(entity3);

    module.addOrReplaceFileScope(fileScope1);

    PackageScope foo = getPackage(module.getRootPackage(), "foo").getChildScope();
    PackageScope fooBar = getPackage(foo, "bar").getChildScope();
    PackageScope fooBarBaz = getPackage(fooBar, "baz").getChildScope();
    assertThat(fooBarBaz.getAllEntities().keys()).containsExactly("entity1");

    // Replace with fileScope2. baz is removed from foo.bar. entity2 from fileScope2 is scopeed.
    module.addOrReplaceFileScope(fileScope2);
    assertThat(fooBar.getAllEntities().keys()).containsExactly("entity2");

    // Replace with fileScope3. foo package is removed. fxx is added with entity3 from fileScope3
    module.addOrReplaceFileScope(fileScope3);
    assertThat(module.getRootPackage().getAllEntities().keys()).containsExactly("fxx");
    PackageScope fxx = getPackage(module.getRootPackage(), "fxx").getChildScope();
    assertThat(fxx.getAllEntities().keys()).containsExactly("entity3");
  }

  private PackageEntity getPackage(EntityScope scope, String simpleName) {
    return getOnlyEntity(scope, simpleName, PackageEntity.class);
  }

  private <T> T getOnlyEntity(EntityScope scope, String simpleName, Class<T> entityClass) {
    List<Entity> entities = scope.getEntitiesWithName(simpleName);
    assertThat(entities).hasSize(1);
    Entity entity = entities.get(0);
    assertThat(entity).isInstanceOf(entityClass);
    @SuppressWarnings("unchecked")
    T typedEntity = (T) entity;
    return typedEntity;
  }
}