package(
    default_visibility = ["//src/main/java/org/javacomp/server:subpackages"],
)

java_library(
    name = "handler",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/org/javacomp/completion",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/logging",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/project",
        "//src/main/java/org/javacomp/project:modulemanager",
        "//src/main/java/org/javacomp/protocol",
        "//src/main/java/org/javacomp/reference",
        "//src/main/java/org/javacomp/server",
        "//src/main/java/org/javacomp/server:request",
        "//src/main/java/org/javacomp/server:requestexception",
        "//third_party:gson",
        "//third_party:guava",
        "//third_party:javac",
        "//third_party:jsr305",
    ],
)
