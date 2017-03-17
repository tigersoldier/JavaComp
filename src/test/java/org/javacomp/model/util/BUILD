java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//src/main/java/org/javacomp/model/util",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "NestedRangeMapBuilderTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
