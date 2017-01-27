java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//external:junit4",
        "//src/main/java/org/javacomp/model",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)

java_test(
    name = "GlobalScopeTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
