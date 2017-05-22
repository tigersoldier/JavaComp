java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "AstScannerTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "AdjustedLineMapTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
