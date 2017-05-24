package org.javacomp.storage;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Ordering;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.ModuleScope;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.testing.TestUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IndexStoreTest {
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final Ordering<TypeReference> TYPE_REFERENCE_SIMPLE_NAME_ORDER =
      Ordering.natural().onResultOf(t -> t.getSimpleName());

  private static final String TEST_DATA_DIR = "src/test/java/org/javacomp/storage/testdata/";
  private static final String TEST_CLASS_FILE = "TestClass.java";
  private static final String OTHER_CLASS_FILE = "OtherClass.java";
  private static final String OTHER_PACKAGE_CLASS_FILE = "other/OtherPackageClass.java";
  private static final List<String> ALL_FILES =
      ImmutableList.of(TEST_CLASS_FILE, OTHER_CLASS_FILE, OTHER_PACKAGE_CLASS_FILE);

  private ModuleScope globalScope = TestUtil.parseFiles(TEST_DATA_DIR, ALL_FILES);
  private IndexStore indexStore = new IndexStore();

  @Test
  public void testSerializeAndDeserialize() {
    IndexStore.SerializedModuleScope serializedModuleScope =
        indexStore.serializeModuleScope(globalScope);
    ModuleScope deserializedModuleScope = indexStore.deserializeModuleScope(serializedModuleScope);

    assertModuleScopesEqual(deserializedModuleScope, globalScope);
  }

  private void assertModuleScopesEqual(ModuleScope deserialized, ModuleScope original) {
    assertSameMemberEntities(
        deserialized.getRootPackage(), original.getRootPackage(), new ArrayDeque<String>());
  }

  private void assertPackagesEqual(
      PackageEntity deserialized, PackageEntity original, Deque<String> qualifiers) {
    assertSameMemberEntities(deserialized.getChildScope(), original.getChildScope(), qualifiers);
  }

  private void assertClassesEqual(
      ClassEntity deserialized, ClassEntity original, Deque<String> qualifiers) {
    assertSameMemberEntities(deserialized, original, qualifiers);
    assertTypesEqual(deserialized.getSuperClass(), original.getSuperClass(), qualifiers);

    List<TypeReference> deserializedInterfaces =
        TYPE_REFERENCE_SIMPLE_NAME_ORDER.immutableSortedCopy(deserialized.getInterfaces());
    List<TypeReference> originalInterfaces =
        TYPE_REFERENCE_SIMPLE_NAME_ORDER.immutableSortedCopy(original.getInterfaces());

    assertThat(deserializedInterfaces)
        .named("interfaces of " + QUALIFIER_JOINER.join(qualifiers))
        .hasSize(originalInterfaces.size());

    for (int i = 0; i < deserializedInterfaces.size(); i++) {
      assertTypesEqual(deserializedInterfaces.get(i), originalInterfaces.get(i), qualifiers);
    }
  }

  private boolean methodsEqual(
      MethodEntity deserialized, MethodEntity original, Deque<String> qualifiers) {
    List<VariableEntity> deserializedParameters = deserialized.getParameters();
    List<VariableEntity> originalParameters = original.getParameters();
    if (deserializedParameters.size() != originalParameters.size()) {
      return false;
    }

    for (int i = 0; i < deserializedParameters.size(); i++) {
      try {
        assertVariablesEqual(deserializedParameters.get(i), originalParameters.get(i), qualifiers);
      } catch (Throwable t) {
        return false;
      }
    }

    try {
      assertTypesEqual(deserialized.getReturnType(), original.getReturnType(), qualifiers);
    } catch (Throwable t) {
      return false;
    }

    return true;
  }

  private void assertVariablesEqual(
      VariableEntity deserialized, VariableEntity original, Deque<String> qualifiers) {
    assertTypesEqual(deserialized.getType(), original.getType(), qualifiers);
  }

  private void assertEntitiesEqual(Entity deserialized, Entity original, Deque<String> qualifiers) {
    assertThat(deserialized.getKind())
        .named("Kind of " + deserialized)
        .isEqualTo(original.getKind());
    if (deserialized instanceof ClassEntity) {
      assertClassesEqual((ClassEntity) deserialized, (ClassEntity) original, qualifiers);
    } else if (deserialized instanceof VariableEntity) {
      assertVariablesEqual((VariableEntity) deserialized, (VariableEntity) original, qualifiers);
    } else if (deserialized instanceof PackageEntity) {
      assertPackagesEqual((PackageEntity) deserialized, (PackageEntity) original, qualifiers);
    } else {
      throw new RuntimeException("Unsupported entity " + deserialized);
    }
  }

  private void assertTypesEqual(
      TypeReference deserialized, TypeReference original, Deque<String> qualifiers) {
    assertThat(QUALIFIER_JOINER.join(deserialized.getFullName()))
        .named("Type of " + QUALIFIER_JOINER.join(qualifiers))
        .contains(QUALIFIER_JOINER.join(original.getFullName()));
  }

  private void assertTypesEqual(
      Optional<TypeReference> deserialized,
      Optional<TypeReference> original,
      Deque<String> qualifiers) {
    assertThat(deserialized.isPresent())
        .named("Presence of type of " + QUALIFIER_JOINER.join(qualifiers));
    if (deserialized.isPresent()) {
      assertTypesEqual(deserialized.get(), original.get(), qualifiers);
    }
  }

  private void assertSameMemberEntities(
      EntityScope deserialized, EntityScope original, Deque<String> qualifiers) {
    assertThat(deserialized.getMemberEntities().keySet())
        .named("Member entities of " + QUALIFIER_JOINER.join(qualifiers))
        .containsExactlyElementsIn(original.getMemberEntities().keySet());

    for (String entityName : deserialized.getMemberEntities().keySet()) {
      qualifiers.add(entityName);
      Collection<Entity> deserializedMembers = deserialized.getMemberEntities().get(entityName);
      Set<Entity> originalMembers = new HashSet<>();
      originalMembers.addAll(deserialized.getMemberEntities().get(entityName));

      assertThat(deserializedMembers)
          .named("entities named '" + entityName + "' in " + QUALIFIER_JOINER.join(qualifiers))
          .hasSize(originalMembers.size());

      for (Entity deserializedMember : deserializedMembers) {
        Entity matchedEntity = null;
        for (Entity originalMember : originalMembers) {
          if (deserializedMember.getKind() != originalMember.getKind()) {
            continue;
          }

          if (deserializedMember.getKind() == Entity.Kind.METHOD) {
            // Special handle of method overloading.
            if (methodsEqual(
                (MethodEntity) deserializedMember, (MethodEntity) originalMember, qualifiers)) {
              matchedEntity = originalMember;
              break;
            }
          } else {
            assertEntitiesEqual(deserializedMember, originalMember, qualifiers);
            matchedEntity = originalMember;
            break;
          }
        } // for orignalMembers

        if (matchedEntity == null) {
          throw new RuntimeException(
              String.format("Entity %s not found in %s", deserializedMember, originalMembers));
        }
      }
      qualifiers.remove();
    }
  }
}
