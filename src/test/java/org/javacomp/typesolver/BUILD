java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/testing",
        "//src/main/java/org/javacomp/typesolver",
        "//third_party:guava",
        "//third_party:javac",
        "//third_party:jsr305",
        "//third_party:junit4",
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

java_test(
    name = "EntityShadowingListBuilderTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
