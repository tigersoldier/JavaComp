java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/file:impl",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "FileSnapshotTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
