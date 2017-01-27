java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//external:junit4",
        "//src/main/java/org/javacomp/completion",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser",
        "//third_party:truth",
    ],
)

java_test(
    name = "CompletorTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
