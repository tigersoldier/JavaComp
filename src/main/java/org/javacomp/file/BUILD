package(
    default_visibility = ["//visibility:public"],
)

PUBLIC_INTERFACES = [
    "FileChangeListener.java",
    "FileManager.java",
    "PathUtils.java",
    "TextPosition.java",
    "TextRange.java",
]

java_library(
    name = "file",
    srcs = PUBLIC_INTERFACES,
    deps = [
        "//third_party:auto_value",
    ],
)

java_library(
    name = "impl",
    srcs = glob(
        ["*.java"],
        exclude = PUBLIC_INTERFACES,
    ),
    deps = [
        ":file",
        "//external:guava",
        "//src/main/java/org/javacomp/logging",
    ],
)
