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
