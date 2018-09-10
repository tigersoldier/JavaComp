package org.javacomp.protocol;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Represents programming constructs like variables, classes, interfaces etc. that appear in a
 * document.
 *
 * <p>Document symbols can be hierarchical and they have two ranges: one that encloses its
 * definition and one that points to its most interesting range, e.g. the range of an identifier.
 */
public class DocumentSymbol implements DocumentSymbolInformation {
  /** The name of this symbol. */
  public String name;

  /** More detail for this symbol, e.g the signature of a function. */
  public @Nullable String detail;

  /** The kind of this symbol. */
  public SymbolKind kind;

  /** Indicates if this symbol is deprecated. */
  public @Nullable boolean deprecated;

  /**
   * The range enclosing this symbol not including leading/trailing whitespace but everything else
   * like comments.
   *
   * <p>This information is typically used to determine if the clients cursor is inside the symbol
   * to reveal in the symbol in the UI.
   */
  public Range range;

  /**
   * The range that should be selected and revealed when this symbol is being picked, e.g the name
   * of a function.
   *
   * <p>Must be contained by the {@link #range}.
   */
  public Range selectionRange;

  /** Children of this symbol, e.g. properties of a class. */
  public @Nullable List<DocumentSymbol> children;

  @Override
  public String toString() {
    return String.format(
        "DocumentSymbol{name=%s,kind=%s,children=%s}",
        name, kind, children != null ? children.size() : 0);
  }
}
