package(
    default_visibility = ["//visibility:public"],
)

PUBLIC_INTERFACES = [
    "FileChangeListener.java",
    "FileManager.java",
    "FileTextLocation.java",
    "PathUtils.java",
    "TextPosition.java",
    "TextRange.java",
    "EditHistory.java",
]

java_library(
    name = "file",
    srcs = PUBLIC_INTERFACES,
    deps = [
        "//third_party:auto_value",
        "//third_party:flogger",
        "//third_party:guava",
    ],
)

java_library(
    name = "impl",
    srcs = glob(
        ["*.java"],
        exclude = PUBLIC_INTERFACES + ["SimpleFileManager.java"],
    ),
    deps = [
        ":file",
        "//third_party:auto_value",
        "//third_party:flogger",
        "//third_party:guava",
    ],
)

java_library(
    name = "simplefilemanager",
    srcs = ["SimpleFileManager.java"],
    deps = [
        ":file",
        "//third_party:guava",
    ],
)
