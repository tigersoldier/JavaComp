package org.javacomp.parser.classfile;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Range;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.model.MethodEntity;
import org.javacomp.model.Module;
import org.javacomp.model.PackageScope;
import org.javacomp.model.TypeReference;
import org.javacomp.model.VariableEntity;
import org.javacomp.parser.classfile.ParsedClassFile.ParsedField;
import org.javacomp.parser.classfile.ParsedClassFile.ParsedMethod;

/** Builder of {@link Module} with classes parsed from .class files. */
public class ClassModuleBuilder {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final Range<Integer> EMPTY_RANGE = Range.closedOpen(0, 0);

  private final ClassFileParser parser;
  private final ClassInfoConverter classInfoConverter;
  private final Module module;

  /** Map from class binary name to class entity. */
  private final Map<String, ClassEntity> classEntityMap;
  /** Map from parent class binary name to parsed class file. */
  private final Multimap<String, ParsedClassFile> parsedInnerClassFileMap;

  public ClassModuleBuilder(Module module) {
    this.parser = new ClassFileParser();
    this.classInfoConverter = new ClassInfoConverter();
    this.module = module;
    this.classEntityMap = new HashMap<>();
    this.parsedInnerClassFileMap = ArrayListMultimap.create();
  }

  public void processClassFile(Path classFilePath) throws IOException {
    InputStream input = Files.newInputStream(classFilePath);
    ClassFileInfo classFileInfo = parser.parse(classFilePath);
    ParsedClassFile parsedClassFile = classInfoConverter.convert(classFileInfo);

    EntityScope parentScope = null;
    if (parsedClassFile.getOuterClassBinaryName().isPresent()) {
      String outerClassBinaryName = parsedClassFile.getOuterClassBinaryName().get();
      if (classEntityMap.containsKey(outerClassBinaryName)) {
        parentScope = classEntityMap.get(outerClassBinaryName);
      }
    } else {
      PackageScope packageScope = module.getOrCreatePackage(parsedClassFile.getClassQualifiers());
      FileScope fileScope =
          FileScope.createFromClassFile(classFilePath, parsedClassFile.getClassQualifiers());
      module.addOrReplaceFileScope(fileScope);
      parentScope = fileScope;
    }

    if (parentScope != null) {
      ClassEntity classEntity = createClassEntity(parsedClassFile, parentScope);
      addClassEntity(parsedClassFile.getClassBinaryName(), classEntity);
    } else {
      // It's an inner class and its outer class is not processed yet.
      parsedInnerClassFileMap.put(parsedClassFile.getOuterClassBinaryName().get(), parsedClassFile);
    }
  }

  private ClassEntity createClassEntity(ParsedClassFile parsedClassFile, EntityScope parentScope) {
    ClassSignature signature = parsedClassFile.getClassSignature();
    ClassEntity classEntity =
        new ClassEntity(
            parsedClassFile.getSimpleName(),
            parsedClassFile.getEntityKind(),
            parsedClassFile.getClassQualifiers(),
            parsedClassFile.isStatic(),
            parentScope,
            Optional.of(signature.getSuperClass()),
            signature.getInterfaces(),
            signature.getTypeParameters(),
            EMPTY_RANGE,
            EMPTY_RANGE);

    ImmutableList<String> classQualifiers =
        new ImmutableList.Builder<String>()
            .addAll(classEntity.getQualifiers())
            .add(classEntity.getSimpleName())
            .build();
    for (ParsedMethod parsedMethod : parsedClassFile.getMethods()) {
      MethodEntity method = createMethodEntity(parsedMethod, classEntity, classQualifiers);
      classEntity.addEntity(method);
    }

    for (ParsedField parsedField : parsedClassFile.getFields()) {
      VariableEntity field = createVariableEntity(parsedField, classEntity, classQualifiers);
      classEntity.addEntity(field);
    }
    return classEntity;
  }

  private MethodEntity createMethodEntity(
      ParsedMethod parsedMethod, ClassEntity parentClass, ImmutableList<String> qualifiers) {
    MethodSignature signature = parsedMethod.getMethodSignature();
    List<VariableEntity> parameters = new ArrayList<>(signature.getParameters().size());
    for (int index = 0; index < signature.getParameters().size(); index++) {
      TypeReference parameterType = signature.getParameters().get(index);
      parameters.add(
          new VariableEntity(
              "arg" + (index + 1),
              Entity.Kind.VARIABLE,
              ImmutableList.of() /* qualifiers */,
              false /* isStatic */,
              parameterType,
              parentClass,
              EMPTY_RANGE,
              EMPTY_RANGE));
    }

    MethodEntity method =
        new MethodEntity(
            parsedMethod.getSimpleName(),
            qualifiers,
            parsedMethod.isStatic(),
            signature.getResult(),
            parameters,
            signature.getTypeParameters(),
            parentClass,
            EMPTY_RANGE,
            EMPTY_RANGE);
    return method;
  }

  private VariableEntity createVariableEntity(
      ParsedField parsedField, ClassEntity parentClass, ImmutableList<String> qualifiers) {
    VariableEntity field =
        new VariableEntity(
            parsedField.getSimpleName(),
            Entity.Kind.FIELD,
            qualifiers,
            parsedField.isStatic(),
            parsedField.getFieldType(),
            parentClass,
            EMPTY_RANGE,
            EMPTY_RANGE);
    return field;
  }

  private void addClassEntity(String binaryName, ClassEntity classEntity) {
    classEntityMap.put(binaryName, classEntity);
    processInnerClasses(binaryName, classEntity);

    EntityScope parentScope = classEntity.getParentScope().get();
    parentScope.addEntity(classEntity);
  }

  private void processInnerClasses(String binaryName, ClassEntity classEntity) {
    if (!parsedInnerClassFileMap.containsKey(binaryName)) {
      return;
    }

    for (ParsedClassFile innerClass : parsedInnerClassFileMap.get(binaryName)) {
      ClassEntity innerClassEntity = createClassEntity(innerClass, classEntity);
      addClassEntity(innerClass.getClassBinaryName(), innerClassEntity);
    }

    parsedInnerClassFileMap.removeAll(binaryName);
  }
}
