java_library(
    name = "tests",
    srcs = glob(["*.java"]),
    data = glob(["testdata/*"]),
    deps = [
        "//external:guava",
        "//external:jsr305",
        "//src/main/java/org/javacomp/server:errorcode",
        "//src/main/java/org/javacomp/server:gsonutils",
        "//src/main/java/org/javacomp/server:request",
        "//src/main/java/org/javacomp/server:requestexception",
        "//src/main/java/org/javacomp/server:response",
        "//src/main/java/org/javacomp/server:serverimpl",
        "//src/main/java/org/javacomp/server/io",
        "//src/main/java/org/javacomp/server/protocol",
        "//third_party:gson",
        "//third_party:junit4",
        "//third_party:mockito",
        "//third_party:truth",
    ],
)

java_test(
    name = "GsonUtilsTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "RequestParserTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)

java_test(
    name = "RequestDispatcherTest",
    size = "small",
    runtime_deps = [
        ":tests",
    ],
)
