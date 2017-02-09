java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//external:junit4",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/testing",
        "//src/main/java/org/javacomp/typesolver",
        "//third_party:truth",
    ],
)

java_test(
    name = "TypeSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "ExpressionSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "OverloadSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
