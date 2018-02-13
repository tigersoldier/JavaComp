java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//third_party:guava",
        "//third_party:javac",
        "//third_party:jsr305",
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
