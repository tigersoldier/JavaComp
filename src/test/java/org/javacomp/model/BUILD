java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//src/main/java/org/javacomp/model",
        "//third_party:guava",
        "//third_party:javac",
        "//third_party:jsr305",
        "//third_party:junit4",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)

java_test(
    name = "ModuleTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
