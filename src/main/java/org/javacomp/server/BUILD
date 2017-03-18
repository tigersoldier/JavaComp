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
    srcs = glob(["*.java"]),
    deps = [
        "//external:guava",
        "//src/main/java/org/javacomp/server/io",
        "//src/main/java/org/javacomp/server/protocol",
        "//third_party:auto_value",
        "//third_party:gson",
    ],
)
