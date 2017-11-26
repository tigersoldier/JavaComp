java_library(
    name = "tests",
    testonly = 1,
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/parser/classfile",
        "//src/main/java/org/javacomp/testing",
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

java_test(
    name = "ClassModuleBuilderTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "SignatureParserTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
