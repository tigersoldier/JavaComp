java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//src/main/java/org/javacomp/model",
        "//third_party:junit4",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)

java_test(
    name = "ModuleScopeTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
