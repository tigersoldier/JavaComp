java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/reference",
        "//src/main/java/org/javacomp/testing",
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
