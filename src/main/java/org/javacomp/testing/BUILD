package(
    default_testonly = 1,
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "testing",
    srcs = glob(["*.java"]),
    deps = [
        "//external:guava",
        "//external:javac",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/parser",
        "//third_party:truth",
    ],
)
