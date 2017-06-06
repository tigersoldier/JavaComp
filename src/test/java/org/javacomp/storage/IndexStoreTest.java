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
import org.javacomp.model.Module;
import org.javacomp.model.PackageEntity;
import org.javacomp.model.TypeReference;
import org.javacomp.model.TypeVariable;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.WildcardTypeVariable;
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

  private Module module = TestUtil.parseFiles(TEST_DATA_DIR, ALL_FILES);
  private IndexStore indexStore = new IndexStore();

  @Test
  public void testSerializeAndDeserialize() {
    IndexStore.SerializedModule serializedModule = indexStore.serializeModule(module);
    Module deserializedModule = indexStore.deserializeModule(serializedModule);

    assertModulesEqual(deserializedModule, module);
  }

  private void assertModulesEqual(Module deserialized, Module original) {
    assertSameMemberEntities(
        deserialized.getRootPackage(), original.getRootPackage(), new ArrayDeque<String>());
  }

  private void assertPackagesEqual(
      PackageEntity deserialized, PackageEntity original, Deque<String> qualifiedName) {
    assertQualifiedName(deserialized, qualifiedName);
    assertThat(deserialized.getQualifiedName()).isEqualTo(original.getQualifiedName());
    assertSameMemberEntities(deserialized.getChildScope(), original.getChildScope(), qualifiedName);
  }

  private void assertClassesEqual(
      ClassEntity deserialized, ClassEntity original, Deque<String> qualifiedName) {
    assertQualifiedName(deserialized, qualifiedName);
    assertThat(deserialized.getQualifiedName()).isEqualTo(original.getQualifiedName());
    assertSameMemberEntities(deserialized, original, qualifiedName);
    assertTypesEqual(deserialized.getSuperClass(), original.getSuperClass(), qualifiedName);

    List<TypeReference> deserializedInterfaces =
        TYPE_REFERENCE_SIMPLE_NAME_ORDER.immutableSortedCopy(deserialized.getInterfaces());
    List<TypeReference> originalInterfaces =
        TYPE_REFERENCE_SIMPLE_NAME_ORDER.immutableSortedCopy(original.getInterfaces());

    assertThat(deserializedInterfaces)
        .named("interfaces of " + QUALIFIER_JOINER.join(qualifiedName))
        .hasSize(originalInterfaces.size());

    for (int i = 0; i < deserializedInterfaces.size(); i++) {
      assertTypesEqual(deserializedInterfaces.get(i), originalInterfaces.get(i), qualifiedName);
    }
  }

  private boolean methodsEqual(
      MethodEntity deserialized, MethodEntity original, Deque<String> qualifiedName) {
    List<VariableEntity> deserializedParameters = deserialized.getParameters();
    List<VariableEntity> originalParameters = original.getParameters();
    if (deserializedParameters.size() != originalParameters.size()) {
      return false;
    }

    // parameters do not have qualified names.
    for (int i = 0; i < deserializedParameters.size(); i++) {
      Deque<String> parameterQualifiedName = new ArrayDeque<>();
      parameterQualifiedName.add(deserializedParameters.get(i).getSimpleName());
      try {
        assertVariablesEqual(
            deserializedParameters.get(i), originalParameters.get(i), parameterQualifiedName);
      } catch (Throwable t) {
        return false;
      }
    }

    try {
      assertTypesEqual(deserialized.getReturnType(), original.getReturnType(), qualifiedName);
    } catch (Throwable t) {
      return false;
    }

    return true;
  }

  private void assertVariablesEqual(
      VariableEntity deserialized, VariableEntity original, Deque<String> qualifiedName) {

    assertTypesEqual(deserialized.getType(), original.getType(), qualifiedName);
    assertQualifiedName(deserialized, qualifiedName);
    assertThat(deserialized.getSimpleName()).isEqualTo(original.getSimpleName());
  }

  private void assertEntitiesEqual(
      Entity deserialized, Entity original, Deque<String> qualifiedName) {
    assertThat(deserialized.getKind())
        .named("Kind of " + deserialized)
        .isEqualTo(original.getKind());
    if (deserialized instanceof ClassEntity) {
      assertClassesEqual((ClassEntity) deserialized, (ClassEntity) original, qualifiedName);
    } else if (deserialized instanceof VariableEntity) {
      assertVariablesEqual((VariableEntity) deserialized, (VariableEntity) original, qualifiedName);
    } else if (deserialized instanceof PackageEntity) {
      assertPackagesEqual((PackageEntity) deserialized, (PackageEntity) original, qualifiedName);
    } else {
      throw new RuntimeException("Unsupported entity " + deserialized);
    }
  }

  private void assertTypesEqual(
      TypeReference deserialized, TypeReference original, Deque<String> qualifiedName) {
    assertThat(QUALIFIER_JOINER.join(deserialized.getFullName()))
        .named("Type of " + QUALIFIER_JOINER.join(qualifiedName))
        .contains(QUALIFIER_JOINER.join(original.getFullName()));
    assertThat(deserialized.getTypeVariables()).hasSize(original.getTypeVariables().size());
    for (int i = 0; i < deserialized.getTypeVariables().size(); i++) {
      TypeVariable deserializedTypeVar = deserialized.getTypeVariables().get(i);
      TypeVariable originalTypeVar = original.getTypeVariables().get(i);
      qualifiedName.addLast("<" + i + "th type var>");
      assertTypeVariablesEqual(deserializedTypeVar, originalTypeVar, qualifiedName);
      qualifiedName.removeLast();
    }
  }

  private void assertTypesEqual(
      Optional<TypeReference> deserialized,
      Optional<TypeReference> original,
      Deque<String> qualifiedName) {
    assertThat(deserialized.isPresent())
        .named("Presence of type of " + QUALIFIER_JOINER.join(qualifiedName))
        .isEqualTo(original.isPresent());
    if (deserialized.isPresent()) {
      assertTypesEqual(deserialized.get(), original.get(), qualifiedName);
    }
  }

  private void assertTypeVariablesEqual(
      TypeVariable deserialized, TypeVariable original, Deque<String> qualifiedName) {
    assertThat(deserialized.getClass()).isEqualTo(original.getClass());
    if (deserialized instanceof TypeReference) {
      assertTypesEqual((TypeReference) deserialized, (TypeReference) original, qualifiedName);
    } else if (deserialized instanceof WildcardTypeVariable) {
      assertWildcardTypeVariablesEqual(
          (WildcardTypeVariable) deserialized, (WildcardTypeVariable) original, qualifiedName);
    } else {
      throw new RuntimeException("Unknown type variable class " + deserialized);
    }
  }

  private void assertWildcardTypeVariablesEqual(
      WildcardTypeVariable deserialized,
      WildcardTypeVariable original,
      Deque<String> qualifiedName) {
    assertThat(deserialized.getBound().isPresent())
        .named("presence of bound of " + QUALIFIER_JOINER.join(qualifiedName))
        .isEqualTo(original.getBound().isPresent());
    if (!deserialized.getBound().isPresent()) {
      return;
    }

    WildcardTypeVariable.Bound deserializedBound = deserialized.getBound().get();
    WildcardTypeVariable.Bound originalBound = original.getBound().get();
    assertThat(deserializedBound.getKind())
        .named("Bound kind of " + QUALIFIER_JOINER.join(qualifiedName))
        .isEqualTo(originalBound.getKind());
    assertTypesEqual(
        deserializedBound.getTypeReference(), originalBound.getTypeReference(), qualifiedName);
  }

  private void assertQualifiedName(Entity entity, Deque<String> qualifiedName) {
    assertThat(entity.getQualifiedName()).isEqualTo(QUALIFIER_JOINER.join(qualifiedName));
  }

  private void assertSameMemberEntities(
      EntityScope deserialized, EntityScope original, Deque<String> qualifiedName) {
    assertThat(deserialized.getMemberEntities().keySet())
        .named("Member entities of " + QUALIFIER_JOINER.join(qualifiedName))
        .containsExactlyElementsIn(original.getMemberEntities().keySet());

    for (String entityName : deserialized.getMemberEntities().keySet()) {
      qualifiedName.addLast(entityName);
      Collection<Entity> deserializedMembers = deserialized.getMemberEntities().get(entityName);
      Set<Entity> originalMembers = new HashSet<>();
      originalMembers.addAll(original.getMemberEntities().get(entityName));

      assertThat(deserializedMembers)
          .named("entities named '" + entityName + "' in " + QUALIFIER_JOINER.join(qualifiedName))
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
                (MethodEntity) deserializedMember, (MethodEntity) originalMember, qualifiedName)) {
              matchedEntity = originalMember;
              break;
            }
          } else {
            assertEntitiesEqual(deserializedMember, originalMember, qualifiedName);
            matchedEntity = originalMember;
            break;
          }
        } // for orignalMembers

        if (matchedEntity == null) {
          throw new RuntimeException(
              String.format("Entity %s not found in %s", deserializedMember, originalMembers));
        }
      }
      qualifiedName.removeLast();
    }
  }
}
