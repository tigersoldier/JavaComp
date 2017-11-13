package org.javacomp.parser.classfile;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class InnerClassEntry {
  /** Binary name of the outer class, e.g. foo/bar/OuterClass1$OuterClass2. */
  public abstract String getOuterClassName();
  /** Simple name of the inner class, e.g. InnerClass. */
  public abstract String getInnerName();

  public static InnerClassEntry create(String outerClassName, String innerName) {
    return new AutoValue_InnerClassEntry(outerClassName, innerName);
  }
}
