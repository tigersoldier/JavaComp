package org.javacomp.model;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.javacomp.model.util.QualifiedNames;

/** A reference to a type for lazy resolution. */
public class TypeReference {
  public static final TypeReference VOID_TYPE =
      new TypeReference("void", ImmutableList.<String>of() /* qualifiers */);
  private final String simpleName;
  private final ImmutableList<String> qualifiers;

  private Symbol resolvedSymbol = null;

  public TypeReference(String simpleName, List<String> qualifiers) {
    this.simpleName = simpleName;
    this.qualifiers = ImmutableList.copyOf(qualifiers);
  }

  public void setResolvedSymbol(Symbol resolvedSymbol) {
    this.resolvedSymbol = resolvedSymbol;
  }

  public String getSimpleName() {
    return this.simpleName;
  }

  public ImmutableList<String> getQualifiers() {
    return this.qualifiers;
  }

  @Override
  public String toString() {
    return "TypeReference<" + QualifiedNames.formatQualifiedName(qualifiers, simpleName) + ">";
  }
}
