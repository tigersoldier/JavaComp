java_library(
    name = "project",
    srcs = glob(["*.java"]),
    resources = [
        "//resources/jdk:index.json",
    ],
    visibility = ["//visibility:public"],
    deps = [
        "//external:guava",
        "//external:javac",
        "//src/main/java/org/javacomp/completion",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/logging",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/reference",
        "//src/main/java/org/javacomp/storage",
    ],
)
