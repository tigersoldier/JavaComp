bind(
    name = "guava",
    actual = "@com_google_guava_guava//jar",
)

bind(
    name = "javac",
    actual = "@com_google_errorprone_javac//jar",
)

maven_jar(
    name = "com_google_errorprone_javac",
    artifact = "com.google.errorprone:javac:1.8.0-u20",
    sha1 = "b23b2b0e3f79e3f737496a9eca5bab65cdca791d",
)

bind(
    name = "jsr305",
    actual = "@com_google_code_findbugs_jsr305//jar",
)

git_repository(
    name = "org_pubref_rules_protobuf",
    remote = "https://github.com/pubref/rules_protobuf",
    tag = "v0.7.1",
)

load("@org_pubref_rules_protobuf//java:rules.bzl", "java_proto_repositories")

java_proto_repositories()

####################
# For tests

bind(
    name = "junit4",
    actual = "@junit_junit//jar",
)

maven_jar(
    name = "junit_junit",
    artifact = "junit:junit:4.12",
    sha1 = "2973d150c0dc1fefe998f834810d68f278ea58ec",
)

bind(
    name = "truth",
    actual = "@com_google_truth_truth//jar",
)

maven_jar(
    name = "com_google_truth_truth",
    artifact = "com.google.truth:truth:0.30",
    sha1 = "9d591b5a66eda81f0b88cf1c748ab8853d99b18b",
)

maven_jar(
    name = "org_mockito_mockito_core",
    artifact = "org.mockito:mockito-core:2.4.2",
    sha1 = "368a656183eac6b47daa37de39ce77b080dac412",
)

maven_jar(
    name = "net_bytebutty_byte_butty",
    artifact = "net.bytebuddy:byte-buddy:1.5.5",
)

maven_jar(
    name = "org_objenesis_objenesis",
    artifact = "org.objenesis:objenesis:2.4",
)

maven_jar(
    name = "com_google_auto_value_auto_value",
    artifact = "com.google.auto.value:auto-value:1.3",
)