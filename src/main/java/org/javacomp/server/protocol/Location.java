package org.javacomp.server.protocol;

import java.net.URI;

/**
 * Represents a location inside a resource, such as a line inside a text file.
 *
 * <p>This class corresponds to the Location type defined by Language Server Protocol:
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#location
 */
public class Location {
  public URI uri;
  public Range range;
}
