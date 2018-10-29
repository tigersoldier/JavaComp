package org.javacomp.protocol.javacomp;

import java.net.URI;
import org.javacomp.protocol.RequestParams;

/**
 * Parameters for "$JavaComp/documentContent" request.
 *
 * <p>The request is used for understanding internal representation of the document content.
 * Response is {@link DocumentContent}.
 */
public class DocumentContentParams implements RequestParams {
  /** The text document's URI. */
  public URI uri;
}
