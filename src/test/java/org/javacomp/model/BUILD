java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//external:junit4",
        "//external:truth",
        "//src/main/java/org/javacomp/model",
        "//third_party:mockito",
    ],
)

java_test(
    name = "GlobalScopeTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
