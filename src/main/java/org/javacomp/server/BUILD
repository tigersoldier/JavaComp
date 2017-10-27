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
    srcs = ["Server.java"],
    deps = [
        "//external:jsr305",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/project",
        "//src/main/java/org/javacomp/protocol",
    ],
)

java_library(
    name = "serverimpl",
    srcs = glob(
        ["*.java"],
        exclude = [
            "ErrorCode.java",
            "GsonUtils.java",
            "Request.java",
            "RequestException.java",
            "Response.java",
            "Server.java",
        ],
    ),
    deps = [
        ":errorcode",
        ":gsonutils",
        ":request",
        ":requestexception",
        ":response",
        ":server",
        "//external:guava",
        "//external:jsr305",
        "//src/main/java/org/javacomp/file",
        "//src/main/java/org/javacomp/file:impl",
        "//src/main/java/org/javacomp/logging",
        "//src/main/java/org/javacomp/options",
        "//src/main/java/org/javacomp/project",
        "//src/main/java/org/javacomp/protocol",
        "//src/main/java/org/javacomp/server/handler",
        "//src/main/java/org/javacomp/server/io",
        "//third_party:auto_value",
        "//third_party:gson",
    ],
)

java_library(
    name = "errorcode",
    srcs = ["ErrorCode.java"],
)

java_library(
    name = "gsonenum",
    srcs = ["GsonEnum.java"],
    visibility = ["//visibility:public"],
)

java_library(
    name = "gsonutils",
    srcs = ["GsonUtils.java"],
    deps = [
        ":gsonenum",
        "//third_party:gson",
    ],
)

java_library(
    name = "request",
    srcs = ["Request.java"],
    deps = [
        "//external:guava",
        "//external:jsr305",
        "//src/main/java/org/javacomp/protocol:nullparams",
        "//src/main/java/org/javacomp/protocol:requestparams",
        "//third_party:auto_value",
        "//third_party:gson",
    ],
)

java_library(
    name = "requestexception",
    srcs = ["RequestException.java"],
    deps = [
        ":errorcode",
    ],
)

java_library(
    name = "response",
    srcs = ["Response.java"],
    deps = [
        ":errorcode",
        "//external:guava",
        "//external:jsr305",
        "//third_party:gson",
    ],
)

java_binary(
    name = "JavaComp",
    main_class = "org.javacomp.server.JavaComp",
    runtime_deps = [":serverimpl"],
)
