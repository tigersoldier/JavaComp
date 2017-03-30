package(
    default_visibility = [":subpackages"],
)

package_group(
    name = "subpackages",
    packages = [
        "//src/main/java/org/javacomp/server/...",
        "//src/test/java/org/javacomp/server/...",
    ],
)

java_library(
    name = "server",
    srcs = glob(
        ["*.java"],
        exclude = [
            "ErrorCode.java",
            "Response.java",
        ],
    ),
    deps = [
        ":errorcode",
        ":response",
        "//external:guava",
        "//external:jsr305",
        "//src/main/java/org/javacomp/logging",
        "//src/main/java/org/javacomp/server/io",
        "//src/main/java/org/javacomp/server/protocol",
        "//third_party:auto_value",
        "//third_party:gson",
    ],
)

java_library(
    name = "errorcode",
    srcs = ["ErrorCode.java"],
)

java_library(
    name = "response",
    srcs = ["Response.java"],
    deps = [
        ":errorcode",
        "//external:guava",
        "//external:jsr305",
    ],
)

java_binary(
    name = "JavaComp",
    main_class = "org.javacomp.server.Server",
    runtime_deps = [":server"],
)
