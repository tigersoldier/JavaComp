package org.javacomp.storage;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PrimitiveEntity;
import org.javacomp.model.SolvedType;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.typesolver.TypeSolver;

/** Storing and loading indexed Java modules from storage. */
public class IndexStore {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final String QUALIFIER_SEPARATOR = "\\.";
  private static final Joiner QUALIFIER_JOINER = Joiner.on(".");
  private static final Range<Integer> EMPTY_RANGE = Range.closedOpen(0, 0);

  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final TypeSolver typeSolver = new TypeSolver();

  private final Map<Entity, Entity> visitedEntities = new HashMap<>();

  private Module module;

  public void writeModuleToFile(Module module, Path filePath) {
    try (BufferedWriter writer = Files.newBufferedWriter(filePath, UTF_8)) {
      this.module = module;
      gson.toJson(serializeModule(module), writer);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      this.module = null;
    }
  }

  public Module readModuleFromFile(Path filePath) {
    try {
      String content = new String(Files.readAllBytes(filePath), UTF_8);
      return deserializeModule(gson.fromJson(content, SerializedModule.class));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public Module readModule(Reader reader) {
    return deserializeModule(gson.fromJson(reader, SerializedModule.class));
  }

  @VisibleForTesting
  SerializedModule serializeModule(Module module) {
    SerializedModule ret = new SerializedModule();
    ret.files =
        module
            .getAllFiles()
            .stream()
            .collect(
                Collectors.groupingBy(
                    fileScope -> QUALIFIER_JOINER.join(fileScope.getPackageQualifiers())))
            .entrySet()
            .stream()
            .map(entry -> serializeFileScopes(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    return ret;
  }

  @VisibleForTesting
  Module deserializeModule(SerializedModule serializedModule) {
    checkNotNull(serializedModule.files, "serializedModule.files");
    Module module = new Module();
    for (SerializedFileScope file : serializedModule.files) {
      module.addOrReplaceFileScope(deserializeFileScope(file));
    }
    return module;
  }

  private SerializedFileScope serializeFileScopes(String packageName, List<FileScope> fileScopes) {
    SerializedFileScope ret = new SerializedFileScope();
    ret.packageName = packageName;
    ret.entities =
        fileScopes
            .stream()
            .flatMap(fileScope -> fileScope.getMemberEntities().values().stream())
            .map(entity -> serializeEntity(entity))
            .collect(Collectors.toList());
    return ret;
  }

  private FileScope deserializeFileScope(SerializedFileScope serializedFileScope) {
    checkNotNull(serializedFileScope.packageName, "serializedFileScope.packageName");
    checkNotNull(serializedFileScope.entities, "serializedFileScope.entities");
    String filename = "//" + serializedFileScope.packageName.replace('.', '/') + ".javacomp";
    List<String> packageQualifiers =
        ImmutableList.copyOf(serializedFileScope.packageName.split(QUALIFIER_SEPARATOR));
    FileScope fileScope = new FileScope(filename, packageQualifiers, null /* compilationUnit */);
    for (SerializedEntity childEntity : serializedFileScope.entities) {
      fileScope.addEntity(deserializeEntity(childEntity, packageQualifiers, fileScope));
    }
    return fileScope;
  }

  private SerializedEntity serializeEntity(Entity entity) {
    SerializedEntity ret;
    if (entity instanceof ClassEntity) {
      ret = serializeClassEntity((ClassEntity) entity);
    } else if (entity instanceof MethodEntity) {
      ret = serializeMethodEntity((MethodEntity) entity);
    } else if (entity instanceof VariableEntity) {
      ret = serializeVariableEntity((VariableEntity) entity);
    } else {
      logger.warning("Unknown Entity: %s", entity);
      ret = new SerializedEntity();
    }
    ret.kind = entity.getKind().name();
    ret.simpleName = entity.getSimpleName();
    return ret;
  }

  private SerializedEntity serializeClassEntity(ClassEntity entity) {
    SerializedEntity ret = new SerializedEntity();
    ret.members =
        entity
            .getMemberEntities()
            .values()
            .stream()
            .map(
                childEntity -> {
                  if (visitedEntities.containsKey(childEntity)) {
                    throw new RuntimeException(
                        "Entity "
                            + childEntity
                            + "Has already been added by "
                            + visitedEntities.get(childEntity)
                            + ", it's being added by "
                            + entity
                            + " again");
                  }
                  visitedEntities.put(childEntity, entity);
                  return serializeEntity(childEntity);
                })
            .collect(Collectors.toList());
    if (entity.getSuperClass().isPresent()) {
      ret.superClass =
          serializeTypeReference(entity.getSuperClass().get(), entity.getParentScope().get());
    }
    ret.interfaces =
        entity
            .getInterfaces()
            .stream()
            .map(t -> serializeTypeReference(t, entity.getParentScope().get()))
            .collect(Collectors.toList());
    return ret;
  }

  private SerializedEntity serializeMethodEntity(MethodEntity entity) {
    SerializedEntity ret = new SerializedEntity();
    ret.parameters =
        entity
            .getParameters()
            .stream()
            .map(param -> serializeEntity(param))
            .collect(Collectors.toList());
    if (!entity.getSimpleName().equals("<init>")) {
      ret.type = serializeTypeReference(entity.getReturnType(), entity.getChildScope());
    }
    return ret;
  }

  private SerializedEntity serializeVariableEntity(VariableEntity entity) {
    SerializedEntity ret = new SerializedEntity();
    ret.type = serializeTypeReference(entity.getType(), entity.getParentScope());
    return ret;
  }

  private Entity deserializeEntity(
      SerializedEntity serializedEntity, List<String> qualifiers, EntityScope parentScope) {
    checkNotNull(serializedEntity.kind, "serializedEntity.kind is null");
    checkNotNull(serializedEntity.simpleName, "serializedEntity.simpleName is null");

    Entity.Kind entityKind = Entity.Kind.valueOf(serializedEntity.kind);
    if (entityKind == null) {
      throw new IllegalArgumentException("Unknown entity kind: " + serializedEntity.kind);
    }

    if (VariableEntity.ALLOWED_KINDS.contains(entityKind)) {
      return deserializeVariableEntity(serializedEntity, entityKind, qualifiers, parentScope);
    } else if (ClassEntity.ALLOWED_KINDS.contains(entityKind)) {
      return deserializeClassEntity(serializedEntity, entityKind, qualifiers, parentScope);
    } else if (entityKind == Entity.Kind.METHOD) {
      checkArgument(
          parentScope instanceof ClassEntity, "parentScope must be ClassEntity for methods.");
      return deserializeMethodEntity(
          serializedEntity, entityKind, qualifiers, (ClassEntity) parentScope);
    } else {
      throw new UnsupportedOperationException("Unsupported entity kind " + entityKind);
    }
  }

  private ClassEntity deserializeClassEntity(
      SerializedEntity serializedEntity,
      Entity.Kind entityKind,
      List<String> qualifiers,
      EntityScope parentScope) {
    Optional<TypeReference> superClass =
        serializedEntity.superClass == null
            ? Optional.empty()
            : Optional.of(deserializeTypeReference(serializedEntity.superClass));
    ImmutableList<TypeReference> interfaces =
        serializedEntity.interfaces == null
            ? ImmutableList.of()
            : serializedEntity
                .interfaces
                .stream()
                .map(t -> deserializeTypeReference(t))
                .collect(ImmutableList.toImmutableList());
    ClassEntity classEntity =
        new ClassEntity(
            serializedEntity.simpleName,
            entityKind,
            qualifiers,
            parentScope,
            superClass,
            interfaces,
            EMPTY_RANGE);
    if (serializedEntity.members != null) {
      List<String> childQualifiers =
          new ImmutableList.Builder<String>()
              .addAll(qualifiers)
              .add(serializedEntity.simpleName)
              .build();
      for (SerializedEntity childEntity : serializedEntity.members) {
        classEntity.addEntity(deserializeEntity(childEntity, childQualifiers, classEntity));
      }
    }
    return classEntity;
  }

  private MethodEntity deserializeMethodEntity(
      SerializedEntity serializedEntity,
      Entity.Kind entityKind,
      List<String> qualifiers,
      ClassEntity classEntity) {
    TypeReference returnType =
        serializedEntity.type == null
            ? TypeReference.EMPTY_TYPE
            : deserializeTypeReference(serializedEntity.type);
    List<VariableEntity> parameters;
    if (serializedEntity.parameters == null) {
      parameters = ImmutableList.of();
    } else {
      List<String> childQualifiers =
          new ImmutableList.Builder<String>()
              .addAll(qualifiers)
              .add(serializedEntity.simpleName)
              .build();
      parameters =
          serializedEntity
              .parameters
              .stream()
              .map(
                  p ->
                      deserializeVariableEntity(
                          p, Entity.Kind.VARIABLE, childQualifiers, classEntity))
              .collect(ImmutableList.toImmutableList());
    }
    return new MethodEntity(
        serializedEntity.simpleName, qualifiers, returnType, parameters, classEntity, EMPTY_RANGE);
  }

  private VariableEntity deserializeVariableEntity(
      SerializedEntity serializedEntity,
      Entity.Kind entityKind,
      List<String> qualifiers,
      EntityScope parentScope) {
    TypeReference type =
        serializedEntity.type == null
            ? TypeReference.EMPTY_TYPE
            : deserializeTypeReference(serializedEntity.type);
    return new VariableEntity(
        serializedEntity.simpleName, entityKind, qualifiers, type, parentScope, EMPTY_RANGE);
  }

  private SerializedType serializeTypeReference(TypeReference type, EntityScope baseScope) {
    SerializedType ret = new SerializedType();
    Optional<SolvedType> optionalSolvedType;
    try {
      optionalSolvedType = typeSolver.solve(type, module, baseScope);
    } catch (Throwable t) {
      logger.warning(t, "Error on solving type %s in %s", type, baseScope);
      optionalSolvedType = Optional.empty();
    }
    if (optionalSolvedType.isPresent()) {
      SolvedType solvedType = optionalSolvedType.get();
      ret.fullName = solvedType.getEntity().getQualifiedName();
      ret.isArray = solvedType.isArray();
    } else {
      ret.fullName = QUALIFIER_JOINER.join(type.getFullName());
      ret.isArray = type.isArray();
    }
    // TODO: serialize type variables.
    return ret;
  }

  private TypeReference deserializeTypeReference(SerializedType type) {
    String fullName = type.fullName != null ? type.fullName : "";
    TypeReference ret =
        TypeReference.builder()
            .setFullName(type.fullName.split(QUALIFIER_SEPARATOR))
            .setPrimitive(PrimitiveEntity.isPrimitive(type.fullName))
            .setArray(type.isArray)
            // TODO: deserialize type variables.
            .setTypeVariables(ImmutableList.of())
            .build();
    return ret;
  }

  @VisibleForTesting
  static class SerializedModule {
    private List<SerializedFileScope> files;
  }

  private static class SerializedFileScope {
    private String packageName;
    private List<SerializedEntity> entities;
  }

  private static class SerializedEntity {
    private String kind;
    private String simpleName;
    private List<SerializedEntity> members;
    private List<SerializedEntity> parameters;
    private SerializedType type;
    private SerializedType superClass;
    private List<SerializedType> interfaces;
  }

  private static class SerializedType {
    private String fullName;
    private boolean isArray;
  }
}
