package org.javacomp.server.protocol;

import java.net.URI;

/**
 * Text documents are identified using a URI.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textdocumentidentifier
 */
public class TextDocumentIdentifier {
  /** The text document's URI. */
  public URI uri;
}
