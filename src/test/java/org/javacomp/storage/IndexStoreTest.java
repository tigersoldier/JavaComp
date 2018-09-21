package org.javacomp.storage;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
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
import org.javacomp.model.TypeArgument;
import org.javacomp.model.TypeParameter;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.model.WildcardTypeArgument;
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
  private static final String FAKE_OBJECT_FILE = "Object.java";
  private static final List<String> ALL_FILES =
      ImmutableList.of(
          TEST_CLASS_FILE, OTHER_CLASS_FILE, OTHER_PACKAGE_CLASS_FILE, FAKE_OBJECT_FILE);

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
    assertSameMemberEntities(deserialized.getScope(), original.getScope(), qualifiedName);
  }

  private void assertClassesEqual(
      ClassEntity deserialized, ClassEntity original, Deque<String> qualifiedName) {
    assertQualifiedName(deserialized, qualifiedName);
    assertThat(deserialized.getQualifiedName()).isEqualTo(original.getQualifiedName());
    assertThat(deserialized.isStatic()).named("isStatic").isEqualTo(original.isStatic());
    assertSameEntities(
        toMultimap(deserialized.getConstructors()),
        toMultimap(original.getConstructors()),
        qualifiedName);
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

    assertTypeParametersEqual(
        deserialized.getTypeParameters(), original.getTypeParameters(), qualifiedName);
  }

  private static Multimap<String, Entity> toMultimap(Collection<? extends Entity> entities) {
    ImmutableMultimap.Builder<String, Entity> builder = new ImmutableMultimap.Builder<>();
    for (Entity entity : entities) {
      builder.put(entity.getSimpleName(), entity);
    }
    return builder.build();
  }

  private boolean methodsEqual(
      MethodEntity deserialized, MethodEntity original, Deque<String> qualifiedName) {
    assertThat(deserialized.isStatic()).named("isStatic").isEqualTo(original.isStatic());
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
    try {
      assertTypeParametersEqual(
          deserialized.getTypeParameters(), original.getTypeParameters(), qualifiedName);
    } catch (Throwable t) {
      return false;
    }

    return true;
  }

  private void assertVariablesEqual(
      VariableEntity deserialized, VariableEntity original, Deque<String> qualifiedName) {
    assertThat(deserialized.isStatic()).named("isStatic").isEqualTo(original.isStatic());
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
        .endsWith(QUALIFIER_JOINER.join(original.getFullName()));
    assertThat(deserialized.getTypeArguments()).hasSize(original.getTypeArguments().size());
    for (int i = 0; i < deserialized.getTypeArguments().size(); i++) {
      TypeArgument deserializedTypeVar = deserialized.getTypeArguments().get(i);
      TypeArgument originalTypeVar = original.getTypeArguments().get(i);
      qualifiedName.addLast("<" + i + "th type var>");
      assertTypeArgumentsEqual(deserializedTypeVar, originalTypeVar, qualifiedName);
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

  private void assertTypeArgumentsEqual(
      TypeArgument deserialized, TypeArgument original, Deque<String> qualifiedName) {
    assertThat(deserialized.getClass()).isEqualTo(original.getClass());
    if (deserialized instanceof TypeReference) {
      assertTypesEqual((TypeReference) deserialized, (TypeReference) original, qualifiedName);
    } else if (deserialized instanceof WildcardTypeArgument) {
      assertWildcardTypeArgumentsEqual(
          (WildcardTypeArgument) deserialized, (WildcardTypeArgument) original, qualifiedName);
    } else {
      throw new RuntimeException("Unknown type argument class " + deserialized);
    }
  }

  private void assertWildcardTypeArgumentsEqual(
      WildcardTypeArgument deserialized,
      WildcardTypeArgument original,
      Deque<String> qualifiedName) {
    assertThat(deserialized.getBound().isPresent())
        .named("presence of bound of " + QUALIFIER_JOINER.join(qualifiedName))
        .isEqualTo(original.getBound().isPresent());
    if (!deserialized.getBound().isPresent()) {
      return;
    }

    WildcardTypeArgument.Bound deserializedBound = deserialized.getBound().get();
    WildcardTypeArgument.Bound originalBound = original.getBound().get();
    assertThat(deserializedBound.getKind())
        .named("Bound kind of " + QUALIFIER_JOINER.join(qualifiedName))
        .isEqualTo(originalBound.getKind());
    assertTypesEqual(
        deserializedBound.getTypeReference(), originalBound.getTypeReference(), qualifiedName);
  }

  private void assertTypeParametersEqual(
      List<TypeParameter> deserialized, List<TypeParameter> original, Deque<String> qualifiedName) {
    String qualifiedNameString = QUALIFIER_JOINER.join(qualifiedName);
    assertThat(deserialized)
        .named("Type parameters of " + qualifiedNameString)
        .hasSize(original.size());

    for (int i = 0; i < deserialized.size(); i++) {
      TypeParameter deserializedParameter = deserialized.get(i);
      TypeParameter originalParameter = original.get(i);
      assertThat(deserializedParameter.getName())
          .named(i + "th type parameter of " + qualifiedNameString)
          .isEqualTo(originalParameter.getName());

      List<TypeReference> deserializedBounds = deserializedParameter.getExtendBounds();
      List<TypeReference> originalBounds = originalParameter.getExtendBounds();
      assertThat(deserializedBounds)
          .named(
              "Bounds of type parameter "
                  + deserializedParameter.getName()
                  + " of "
                  + qualifiedNameString)
          .hasSize(originalBounds.size());

      for (int j = 0; j < deserializedBounds.size(); j++) {
        qualifiedName.addLast(j + "th bound of type parameter " + deserializedParameter.getName());
        assertTypesEqual(deserializedBounds.get(j), originalBounds.get(j), qualifiedName);
        qualifiedName.removeLast();
      }
    }
  }

  private void assertQualifiedName(Entity entity, Deque<String> qualifiedName) {
    assertThat(entity.getQualifiedName()).isEqualTo(QUALIFIER_JOINER.join(qualifiedName));
  }

  private void assertSameMemberEntities(
      EntityScope deserialized, EntityScope original, Deque<String> qualifiedName) {
    assertSameEntities(
        deserialized.getMemberEntities(), original.getMemberEntities(), qualifiedName);
  }

  private void assertSameEntities(
      Multimap<String, Entity> deserialized,
      Multimap<String, Entity> original,
      Deque<String> qualifiedName) {
    assertThat(deserialized.keySet())
        .named("Member entities of " + QUALIFIER_JOINER.join(qualifiedName))
        .containsExactlyElementsIn(original.keySet());

    for (String entityName : deserialized.keySet()) {
      qualifiedName.addLast(entityName);
      Collection<Entity> deserializedMembers = deserialized.get(entityName);
      Set<Entity> originalMembers = new HashSet<>();
      originalMembers.addAll(original.get(entityName));

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
