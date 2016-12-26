java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:junit4",
        "//external:truth",
        "//src/main/java/org/javacomp/model/util",
    ],
)

java_test(
    name = "NestedRangeMapBuilderTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
