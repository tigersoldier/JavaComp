java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//external:junit4",
        "//external:truth",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//src/main/java/org/javacomp/typesolver",
    ],
)

java_test(
    name = "TypeSolverTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
