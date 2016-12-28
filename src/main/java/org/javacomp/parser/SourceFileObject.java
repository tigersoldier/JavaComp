package org.javacomp.parser;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/** A {@link SimpleJavaFileObject} for Java source code. */
public class SourceFileObject extends SimpleJavaFileObject {
  /** @param filename the absolute path of the source file. */
  public SourceFileObject(String filename) {
    super(URI.create("file://" + filename), Kind.SOURCE);
  }

  @Override
  public CharSequence getCharContent(boolean ignoreEncodingErrors) {
    return "";
  }

}
