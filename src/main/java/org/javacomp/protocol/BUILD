package(
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "protocol",
    srcs = glob(
        ["*.java"],
        exclude = ["ReuestParams.java"],
    ),
    deps = [
        ":requestparams",
        "//external:guava",
        "//external:javac",
        "//external:jsr305",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/server:gsonenum",
        "//third_party:gson",
    ],
)

java_library(
    name = "requestparams",
    srcs = ["RequestParams.java"],
)

java_library(
    name = "nullparams",
    srcs = ["NullParams.java"],
    deps = [":requestparams"],
)
