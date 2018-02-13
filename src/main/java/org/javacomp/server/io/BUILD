package(
    default_visibility = ["//src/main/java/org/javacomp/server:subpackages"],
)

java_library(
    name = "io",
    srcs = glob(["*.java"]),
    deps = [
        "//src/main/java/org/javacomp/logging",
        "//src/main/java/org/javacomp/server:response",
        "//third_party:gson",
        "//third_party:guava",
    ],
)
