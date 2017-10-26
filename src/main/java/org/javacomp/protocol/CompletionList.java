package org.javacomp.protocol;

import java.util.List;

/**
 * Represents a collection of {@link CompletionItem} instances to be presented in the editor.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textDocument_completion
 */
public class CompletionList implements RequestParams {
  /** This list it not complete. Further typing should result in recomputing this list. */
  public boolean isIncomplete;

  /** The completion items. */
  public List<CompletionItem> items;
}
