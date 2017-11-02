java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//src/main/java/org/javacomp/parser/classfile",
        "//third_party:auto_value",
        "//third_party:junit4",
        "//third_party:truth",
    ],
)

java_test(
    name = "ClassFileParserTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
