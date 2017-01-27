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

java_library(
    name = "auto_value",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_value_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "@com_google_auto_value_auto_value//jar",
    ],
)

java_plugin(
    name = "auto_annotation_plugin",
    processor_class = "com.google.auto.value.processor.AutoAnnotationProcessor",
    tags = [
        "annotation=com.google.auto.value.AutoAnnotation;" +
        "genclass=${package}.AutoAnnotation_${outerclasses}${classname}_${methodname}",
    ],
    deps = [
        "@com_google_auto_value_auto_value//jar",
    ],
)

java_plugin(
    name = "auto_value_plugin",
    processor_class = "com.google.auto.value.processor.AutoValueProcessor",
    tags = [
        "annotation=com.google.auto.value.AutoValue;" +
        "genclass=${package}.AutoValue_${outerclasses}${classname}",
    ],
    deps = [
        "@com_google_auto_value_auto_value//jar",
    ],
)

java_library(
    name = "truth",
    exports = [
        "@com_google_truth_extensions_truth_java8_extension//jar",
        "@com_google_truth_truth//jar",
    ],
)
