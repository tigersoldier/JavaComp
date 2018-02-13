java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/**"]),
    deps = [
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/storage",
        "//src/main/java/org/javacomp/testing",
        "//third_party:guava",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "IndexStoreTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
