package(
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "project",
    srcs = [
        "Project.java",
    ],
    resources = [
        "//resources/jdk:index.json",
    ],
    visibility = ["//visibility:public"],
    deps = [
        ":modulemanager",
        ":modulemanagerimpl",
        "//src/main/java/org/javacomp/completion",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/protocol",
        "//src/main/java/org/javacomp/reference",
        "//src/main/java/org/javacomp/storage",
        "//third_party:flogger",
        "//third_party:guava",
    ],
)

java_library(
    name = "modulemanager",
    srcs = [
        "FileItem.java",
        "ModuleManager.java",
    ],
    deps = [
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//third_party:auto_value",
    ],
)

java_library(
    name = "modulemanagerimpl",
    srcs = [
        "FileSystemModuleManager.java",
    ],
    deps = [
        ":modulemanager",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/parser/classfile",
        "//third_party:auto_value",
        "//third_party:flogger",
        "//third_party:guava",
        "//third_party:javac",
    ],
)

java_library(
    name = "positioncontext",
    srcs = [
        "PositionContext.java",
    ],
    deps = [
        ":modulemanager",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//third_party:auto_value",
        "//third_party:flogger",
        "//third_party:javac",
    ],
)

java_library(
    name = "simplemodulemanager",
    srcs = ["SimpleModuleManager.java"],
    deps = [
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/file:simplefilemanager",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/project:modulemanager",
        "//third_party:auto_value",
        "//third_party:guava",
    ],
)
