java_binary(
    name = "AstPrinter",
    srcs = ["AstPrinter.java"],
    main_class = "org.javacomp.tool.AstPrinter",
    deps = [
        "//external:guava",
        "//external:javac",
        "//src/main/java/org/javacomp/parser",
    ],
)

java_binary(
    name = "IndexPrinter",
    srcs = ["IndexPrinter.java"],
    main_class = "org.javacomp.tool.IndexPrinter",
    deps = [
        "//external:guava",
        "//external:javac",
        "//src/main/java/org/javacomp/model",
        "//src/main/java/org/javacomp/model/util",
        "//src/main/java/org/javacomp/project",
    ],
)
