package org.javacomp.server.protocol;

/**
 * An identifier to denote a specific version of a text document.
 *
 * <p>See:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#versionedtextdocumentidentifier
 */
public class VersionedTextDocumentIdentifier extends TextDocumentIdentifier {
  /** The version number of this document. */
  public int version;
}
