package org.javacomp.protocol;

/**
 * A parameter literal used in requests to resolve project-wide references for the symbol denoted by
 * the given text document position.
 *
 * <p>See:
 * https://microsoft.github.io/language-server-protocol/specification#textDocument_references
 */
public class ReferenceParams extends TextDocumentPositionParams {

  public ReferenceContext context;

  public static class ReferenceContext {
    /** Include the declaration of the current symbol. */
    public boolean includeDeclaration;
  }
}
