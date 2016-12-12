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
