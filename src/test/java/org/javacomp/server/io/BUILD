java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:jsr305",
        "//src/main/java/org/javacomp/server/io",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "RequestReaderTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
