package(
    default_visibility = ["//src/main/java/org/javacomp/server:subpackages"],
)

java_library(
    name = "protocol",
    srcs = glob(
        ["*.java"],
        exclude = ["ReuestParams.java"],
    ),
    deps = [
        ":requestparams",
        "//external:jsr305",
        "//src/main/java/org/javacomp/completion",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/project",
        "//src/main/java/org/javacomp/server",
        "//src/main/java/org/javacomp/server:gsonenum",
        "//src/main/java/org/javacomp/server:request",
        "//src/main/java/org/javacomp/server:requestexception",
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
