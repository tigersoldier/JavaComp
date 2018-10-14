java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/file:simplefilemanager",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/project:simplemodulemanager",
        "//src/main/java/org/javacomp/reference",
        "//src/main/java/org/javacomp/testing",
        "//third_party:guava",
        "//third_party:javac",
        "//third_party:jsr305",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "DefinitionSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "SignatureSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "ReferenceSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
