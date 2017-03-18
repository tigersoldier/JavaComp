package org.javacomp.server;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import org.javacomp.server.io.NotEnoughDataException;
import org.javacomp.server.io.RequestReader;

/**
 * A parser for Request that follows the base protocol of Microsoft Language Server Protocol.
 *
 * <p>Note: the header keys are lower-cased.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#base-protocol
 */
public class RequestParser {
  /** Normalized Content-Length header name in the request header. */
  public static final String HEADER_CONTENT_LENGTH = "content-length";
  /** Normalized Content-Type header name in the request header. */
  public static final String HEADER_CONTENT_TYPE = "content-type";

  private static final int MAX_CONTENT_LENGTH = 1024 * 1024;
  private static final String HEADER_KEY_VALUE_SEPARATOR = ": ";

  private final Gson gson;

  public RequestParser() {
    this.gson = new Gson();
  }

  /** Parse a request from input stream. */
  public RawRequest parse(RequestReader reader) throws ParseException {
    Map<String, String> header = parseHeader(reader);
    int contentLength = getContentLength(header);
    RawRequest.Content content = parseContent(reader, contentLength);
    return RawRequest.create(header, content);
  }

  private Map<String, String> parseHeader(RequestReader reader) throws ParseException {
    ImmutableMap.Builder<String, String> headerBuilder = new ImmutableMap.Builder<>();
    while (true) {
      String headerLine;
      try {
        headerLine = reader.readLine();
      } catch (NotEnoughDataException e) {
        throw new ParseException(ErrorCode.INVALID_REQUEST, e, "No header found.");
      } catch (IOException e) {
        throw new ParseException(ErrorCode.INVALID_REQUEST, e, "Failed to read request.");
      }

      if (headerLine == null) {
        throw new ParseException(
            ErrorCode.PARSE_ERROR, "Failed to read header. Incomplete request");
      }

      if (headerLine.length() == 0) {
        // We enconter the header/content separator. Stop reading and parsing header.
        break;
      }

      String[] parts = headerLine.split(HEADER_KEY_VALUE_SEPARATOR, 2);
      if (parts.length != 2) {
        throw new ParseException(ErrorCode.PARSE_ERROR, "Malformed header %s", headerLine);
      }

      headerBuilder.put(parts[0].toLowerCase(), parts[1]);
    }
    return headerBuilder.build();
  }

  private int getContentLength(Map<String, String> header) throws ParseException {
    if (!header.containsKey(HEADER_CONTENT_TYPE)) {
      throw new ParseException(ErrorCode.INVALID_REQUEST, "Missing Content-Length header");
    }

    int contentLength;
    try {
      contentLength = Integer.parseInt(header.get(HEADER_CONTENT_LENGTH));
    } catch (NumberFormatException e) {
      throw new ParseException(
          ErrorCode.PARSE_ERROR, "Invalid content length: %s", header.get(HEADER_CONTENT_LENGTH));
    }

    if (contentLength <= 0) {
      throw new ParseException(
          ErrorCode.INVALID_REQUEST,
          "Content length should be positive, but got %s",
          contentLength);
    }

    return contentLength;
  }

  private RawRequest.Content parseContent(RequestReader reader, int contentLength)
      throws ParseException {
    if (contentLength > MAX_CONTENT_LENGTH) {
      throw new ParseException(
          ErrorCode.INVALID_REQUEST,
          "Content length %s exceeds the limit %s",
          contentLength,
          MAX_CONTENT_LENGTH);
    }

    String content;
    try {
      content = reader.readString(contentLength);
    } catch (NotEnoughDataException e) {
      throw new ParseException(
          ErrorCode.PARSE_ERROR, e, "Request ended without enough content data.");
    } catch (IOException e) {
      throw new ParseException(ErrorCode.PARSE_ERROR, e, "Failed to read request.");
    }

    RawRequest.Content requestContent = gson.fromJson(content, RawRequest.Content.class);
    checkRequestContentIsValid(requestContent);
    return requestContent;
  }

  /** Checks the required fields are present in the request content. */
  private void checkRequestContentIsValid(RawRequest.Content content) throws ParseException {
    if (Strings.isNullOrEmpty(content.getMethod())) {
      throw new ParseException(ErrorCode.INVALID_REQUEST, "Missing request method.");
    }

    if (Strings.isNullOrEmpty(content.getId())) {
      throw new ParseException(ErrorCode.INVALID_REQUEST, "Missing request ID.");
    }

    if (content.getParams() == null) {
      throw new ParseException(ErrorCode.INVALID_REQUEST, "Missing request params.");
    }

    // We don't check the jsonrpc field because it's useless.
  }
}
