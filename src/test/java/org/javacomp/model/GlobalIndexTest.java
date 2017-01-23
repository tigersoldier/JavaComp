package org.javacomp.model;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
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
public class GlobalIndexTest {
  @Rule public MockitoRule mrule = MockitoJUnit.rule();

  @Mock private Entity entity1;
  @Mock private Entity entity2;
  @Mock private Entity entity3;
  @Mock private Entity entity4;

  @Before
  public void setUpMocks() {
    when(entity1.getSimpleName()).thenReturn("entity1");
    when(entity2.getSimpleName()).thenReturn("entity2");
    when(entity3.getSimpleName()).thenReturn("entity3");
    when(entity4.getSimpleName()).thenReturn("entity4");
  }

  private GlobalIndex globalIndex = new GlobalIndex();

  @Test
  public void addFilesShouldCreatePackages() {
    FileIndex fileIndex1 = new FileIndex("filename1", ImmutableList.of("foo", "bar"));
    FileIndex fileIndex2 = new FileIndex("filename2", ImmutableList.of("foo", "bar", "baz"));
    FileIndex fileIndex3 = new FileIndex("filename3", ImmutableList.of("foo", "baz"));
    FileIndex fileIndex4 = new FileIndex("filename4", ImmutableList.of("fxx"));
    fileIndex1.addEntity(entity1);
    fileIndex2.addEntity(entity2);
    fileIndex3.addEntity(entity3);
    fileIndex4.addEntity(entity4);

    globalIndex.addOrReplaceFileIndex(fileIndex1);
    globalIndex.addOrReplaceFileIndex(fileIndex2);
    globalIndex.addOrReplaceFileIndex(fileIndex3);
    globalIndex.addOrReplaceFileIndex(fileIndex4);

    assertThat(globalIndex.getAllEntities().keys()).containsExactly("foo", "fxx");

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    PackageIndex fooBarBaz = getPackage(fooBar, "baz").getChildIndex();
    PackageIndex fooBaz = getPackage(foo, "baz").getChildIndex();
    PackageIndex fxx = getPackage(globalIndex, "fxx").getChildIndex();

    assertThat(foo.getAllEntities().keys()).containsExactly("bar", "baz");
    assertThat(fooBar.getAllEntities().keys()).containsExactly("baz", "entity1");
    assertThat(fooBarBaz.getAllEntities().keys()).containsExactly("entity2");
    assertThat(fooBaz.getAllEntities().keys()).containsExactly("entity3");
    assertThat(fxx.getAllEntities().keys()).containsExactly("entity4");
  }

  @Test
  public void replaceFileWithSamePackageShouldNotRemovePackage() {
    FileIndex fileIndex1 = new FileIndex("foobar", ImmutableList.of("foo", "bar"));
    FileIndex fileIndex2 = new FileIndex("foobar", ImmutableList.of("foo", "bar"));

    fileIndex1.addEntity(entity1);
    fileIndex2.addEntity(entity2);

    globalIndex.addOrReplaceFileIndex(fileIndex1);
    globalIndex.addOrReplaceFileIndex(fileIndex2);

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    assertThat(fooBar.getAllEntities().keys()).containsExactly("entity2");
  }

  @Test
  public void replaceFileWithDifferentPackageShouldNotRemovePackage() {
    FileIndex fileIndex1 = new FileIndex("foobar", ImmutableList.of("foo", "bar", "baz"));
    FileIndex fileIndex2 = new FileIndex("foobar", ImmutableList.of("foo", "bar"));
    FileIndex fileIndex3 = new FileIndex("foobar", ImmutableList.of("fxx"));

    fileIndex1.addEntity(entity1);
    fileIndex2.addEntity(entity2);
    fileIndex3.addEntity(entity3);

    globalIndex.addOrReplaceFileIndex(fileIndex1);

    PackageIndex foo = getPackage(globalIndex, "foo").getChildIndex();
    PackageIndex fooBar = getPackage(foo, "bar").getChildIndex();
    PackageIndex fooBarBaz = getPackage(fooBar, "baz").getChildIndex();
    assertThat(fooBarBaz.getAllEntities().keys()).containsExactly("entity1");

    // Replace with fileIndex2. baz is removed from foo.bar. entity2 from fileIndex2 is indexed.
    globalIndex.addOrReplaceFileIndex(fileIndex2);
    assertThat(fooBar.getAllEntities().keys()).containsExactly("entity2");

    // Replace with fileIndex3. foo package is removed. fxx is added with entity3 from fileIndex3
    globalIndex.addOrReplaceFileIndex(fileIndex3);
    assertThat(globalIndex.getAllEntities().keys()).containsExactly("fxx");
    PackageIndex fxx = getPackage(globalIndex, "fxx").getChildIndex();
    assertThat(fxx.getAllEntities().keys()).containsExactly("entity3");
  }

  private PackageEntity getPackage(EntityIndex index, String simpleName) {
    return getOnlyEntity(index, simpleName, PackageEntity.class);
  }

  private <T> T getOnlyEntity(EntityIndex index, String simpleName, Class<T> entityClass) {
    List<Entity> entities = index.getEntitiesWithName(simpleName);
    assertThat(entities).hasSize(1);
    Entity entity = entities.get(0);
    assertThat(entity).isInstanceOf(entityClass);
    @SuppressWarnings("unchecked")
    T typedEntity = (T) entity;
    return typedEntity;
  }
}
