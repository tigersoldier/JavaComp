java_library(
    name = "javacomp",
    srcs = glob(["*.java"]),
    visibility = ["//visibility:public"],
    deps = [
        "//external:guava",
        "//external:javac",
        "//src/main/java/org/javacomp/model:symbol_index_scope",
        "//src/main/java/org/javacomp/parser:ast_scanner",
        "//src/main/proto:symbol_proto",
    ],
)
