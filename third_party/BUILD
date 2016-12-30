# Export external packages defined in WORKSPACE.
# Some Maven artifacts have compiletime or runtime dependencies. Bazel's bind()
# rule does not support multiple artifacts. Defining rules in third_party is
# more flexible.
package(
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "mockito",
    exports = [
        "@org_mockito_mockito_core//jar",
    ],
    runtime_deps = [
        "@net_bytebutty_byte_butty//jar",
        "@org_objenesis_objenesis//jar",
    ],
)
