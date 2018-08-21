# Export external packages defined in WORKSPACE.
# Some Maven artifacts have compiletime or runtime dependencies. Bazel's bind()
# rule does not support multiple artifacts. Defining rules in third_party is
# more flexible.
package(
    default_visibility = ["//visibility:public"],
)

java_library(
    name = "auto_value",
    exported_plugins = [
        ":auto_annotation_plugin",
        ":auto_value_plugin",
    ],
    visibility = ["//visibility:public"],
    exports = [
        "@com_google_auto_value_auto_value_annotations//jar",
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
    name = "gson",
    exports = [
        "@com_google_code_gson_gson//jar",
    ],
)

java_library(
    name = "guava",
    exports = [
        "@com_google_guava_guava21//jar",
    ],
)

java_library(
    name = "javac",
    exports = [
        "@com_google_errorprone_javac//jar",
    ],
)

java_library(
    name = "jsr305",
    exports = [
        "@com_google_code_findbugs_jsr305//jar",
    ],
)

####################
# For tests

java_library(
    name = "truth",
    testonly = 1,
    exports = [
        "@com_google_truth_extensions_truth_java8_extension//jar",
        "@com_google_truth_truth//jar",
    ],
)

java_library(
    name = "junit4",
    testonly = 1,
    exports = [
        "@junit_junit//jar",
        "@org_hamcrest_hamcrest_core_1_3//jar",
    ],
)

java_library(
    name = "mockito",
    testonly = 1,
    exports = [
        "@org_mockito_mockito_core//jar",
    ],
    runtime_deps = [
        "@net_bytebutty_byte_butty//jar",
        "@org_objenesis_objenesis//jar",
    ],
)
