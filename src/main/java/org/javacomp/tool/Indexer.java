package org.javacomp.tool;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.javacomp.file.FileManager;
import org.javacomp.file.PathUtils;
import org.javacomp.file.SimpleFileManager;
import org.javacomp.model.FileScope;
import org.javacomp.model.Module;
import org.javacomp.options.IndexOptions;
import org.javacomp.parser.AstScanner;
import org.javacomp.parser.ParserContext;
import org.javacomp.parser.classfile.ClassModuleBuilder;
import org.javacomp.project.Project;
import org.javacomp.storage.IndexStore;

/**
 * Creates index files for specified source code.
 *
 * <p>Usage: Indexer <root path> <output file> <ignored paths...>
 */
public class Indexer {

  private final ParserContext parserContext = new ParserContext();

  public Indexer() {}

  public void run(
      List<String> inputPaths,
      String outputPath,
      List<String> ignorePaths,
      List<String> dependIndexFiles,
      boolean withJdk) {
    Project project =
        new Project(
            new SimpleFileManager(Paths.get(inputPaths.get(0)), ignorePaths),
            Paths.get(inputPaths.get(0)).toUri(),
            IndexOptions.PUBLIC_READONLY_BUILDER.build());
    // Do not initialize the project. We handle the files on our own.
    for (String inputPath : inputPaths) {
      Path path = Paths.get(inputPath);
      FileManager fileManager = new SimpleFileManager(path, ignorePaths);
      ClassModuleBuilder classModuleBuilder = new ClassModuleBuilder(project.getModule());
      ImmutableMap<String, Consumer<Path>> handlers =
          ImmutableMap.<String, Consumer<Path>>of(
              ".class",
              subpath -> classModuleBuilder.processClassFile(subpath),
              ".java",
              subpath -> addJavaFile(subpath, project.getModule(), fileManager));
      if (Files.isDirectory(path)) {
        PathUtils.walkDirectory(
            path,
            handlers,
            /* ignorePredicate= */ subpath -> fileManager.shouldIgnorePath(subpath));
      } else if (inputPath.endsWith(".jar")) {
        try {
          PathUtils.walkDirectory(
              PathUtils.getRootPathForJarFile(path),
              handlers,
              /* ignorePredicate= */ subpath -> false);
        } catch (IOException t) {
          throw new RuntimeException(t);
        }
      }
    }
    for (String dependIndexFile : dependIndexFiles) {
      project.loadTypeIndexFile(dependIndexFile);
    }
    if (withJdk) {
      project.loadJdkModule();
    }
    new IndexStore().writeModuleToFile(project.getModule(), Paths.get(outputPath));
  }

  private void addJavaFile(Path path, Module module, FileManager fileManager) {
    Optional<CharSequence> content = fileManager.getFileContent(path);
    FileScope fileScope =
        new AstScanner(IndexOptions.PUBLIC_READONLY_BUILDER.build())
            .startScan(
                parserContext.parse(path.toString(), content.get()),
                path.toString(),
                content.get());
    module.addOrReplaceFileScope(fileScope);
  }

  public static void main(String[] args) {
    if (args.length < 3) {
      System.out.println(
          "Usage: Indexer <directory or jar file>[, directory or jar file...]  -o <output file> [options]");
      System.out.println("  Options:");
      System.out.println("    --depend|-d <index files...>");
      System.out.println("    --ignore|-i <ignored paths...>]");
      System.out.println("    --no-jdk      Do not load JDK module.");
      return;
    }
    String outputPath = null;
    List<String> inputPaths = new ArrayList<>();
    List<String> ignorePaths = new ArrayList<>();
    List<String> dependIndexPaths = new ArrayList<>();
    List<String> currentList = inputPaths;
    boolean withJdk = true;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if ("-o".equals(arg)) {
        if (i + 1 < args.length) {
          outputPath = args[i + 1];
          i++;
          currentList = null;
        }
      } else if ("--depend".equals(arg) || "-d".equals(arg)) {
        currentList = dependIndexPaths;
      } else if ("--ignore".equals(arg) || "-i".equals(arg)) {
        currentList = ignorePaths;
      } else if ("--no-jdk".equals(arg)) {
        withJdk = false;
      } else if (currentList == null) {
        System.err.println("-o only accepts one value");
        System.exit(1);
      } else {
        currentList.add(arg);
      }
    }

    if (outputPath == null) {
      System.err.println("-o must be specified with one value");
      System.exit(1);
    }
    if (inputPaths.isEmpty()) {
      System.err.println("One or more input file must be specified");
      System.exit(1);
    }

    new Indexer().run(inputPaths, outputPath, ignorePaths, dependIndexPaths, withJdk);
  }
}
