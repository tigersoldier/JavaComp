package org.javacomp.protocol;

import javax.annotation.Nullable;

/** Represents information about programming constructs like variables, classes, interfaces etc. */
public class SymbolInformation implements DocumentSymbolInformation {
  /** The name of this symbol. */
  public String name;

  /** The kind of this symbol. */
  public SymbolKind kind;

  /** Indicates if this symbol is deprecated. */
  @Nullable public Boolean deprecated;

  /**
   * The location of this symbol.
   *
   * <p>The location's range is used by a tool to reveal the location in the editor. If the symbol
   * is selected in the tool the range's start information is used to position the cursor. So the
   * range usually spans more then the actual symbol's name and does normally include things like
   * visibility modifiers.
   *
   * <p>The range doesn't have to denote a node range in the sense of a abstract syntax tree. It can
   * therefore not be used to re-construct a hierarchy of the symbols.
   */
  public Location location;

  /**
   * The name of the symbol containing this symbol.
   *
   * <p>This information is for user interface purposes (e.g. to render a qualifier in the user
   * interface if necessary). It can't be used to re-infer a hierarchy for the document symbols.
   */
  @Nullable public String containerName;
}
