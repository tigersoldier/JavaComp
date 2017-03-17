package org.javacomp.server.io;

import static com.google.common.base.Preconditions.checkArgument;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import org.javacomp.logging.JLogger;

/**
 * A reader wrapping around {@link InputStream} providing useful methods for parsing requests.
 *
 * <p>The underlying InputStream is closed when this reader is closed. The stream is assumbed to be
 * UTF-8 encoded.
 *
 * <p>The reader is backed by a ring buffer.
 */
public class RequestReader implements Closeable {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private final InputStream inputStream;

  // Ring buffer. readOffset is index to the first byte to read. writeOffset is the index to the
  // first byte to write. bufferSize is the size of the byte array.
  //
  // The buffer is empty when readOffset == writeOffset. The buffer is full if
  //   readOffset > 0 && writeOffset == readOffset - 1, or
  //   readOffset == 0 && writeOffset == bufferSize - 1.
  // This means the capacity of the buffer is bufferSize - 1.
  private int readOffset;
  private int writeOffset;
  private final int bufferSize;
  private final byte[] buffer;

  public RequestReader(InputStream inputStream, int capacity) {
    checkArgument(capacity > 0, "Capacity must be positive.");
    this.inputStream = inputStream;
    this.bufferSize = capacity + 1;
    this.buffer = new byte[this.bufferSize];
    this.readOffset = 0;
    this.writeOffset = 0;
  }

  /**
   * Reads a line ending with \r, \n, or \r\n.
   *
   * @return the next line, or {@code null} if the input stream ends without any line terminators.
   *     Note that even if there is data left in the buffer, as long as no line terminator is found
   *     and the stream is closed, the return value is {@code null}, not the rest of the content.
   *     Empty lines are returned as empty strings, not {@code null}.
   * @throw NotEnoughDataException thrown if the input stream ends without a full line
   * @throw IOException thrown if any other I/O errors occur
   */
  public String readLine() throws NotEnoughDataException, IOException {
    int peekOffset = readOffset;
    int lineLen = 0;
    int terminatorLen = 0;
    StringBuilder sb = new StringBuilder();
    while (true) {
      if (peekOffset == writeOffset) {
        if (getRemainCapacity() == 0) {
          sb.append(readString(lineLen));
          lineLen = 0;
        }
        if (!fillBuffer()) {
          throw new NotEnoughDataException("Input stream ends without line terminator.");
        }
      }
      if (buffer[peekOffset] == '\n' || buffer[peekOffset] == '\r') {
        terminatorLen = 1;
        break;
      } else {
        peekOffset = incrementOffset(peekOffset, 1);
        lineLen++;
      }
    }

    if (lineLen > 0) {
      sb.append(readString(lineLen));
    }
    String line = sb.toString();

    // Checking \r\n. This needs to be done after the line is read to handle the case where
    // the length of the line is capacity - 1.
    if (buffer[peekOffset] == '\r') {
      peekOffset = incrementOffset(peekOffset, 1);
      if (peekOffset == writeOffset && !fillBuffer()) {
        // At the end of the stream, fine.
        terminatorLen = 1;
      } else {
        terminatorLen = buffer[peekOffset] == '\n' ? 2 : 1;
      }
    }
    readOffset = incrementOffset(readOffset, terminatorLen);
    return line;
  }

  /**
   * Reads the next numBytes bytes and return them as a string.
   *
   * <p>This method will block until numBytes bytes are read, or the input stream has ended.
   *
   * @throw NotEnoughDataException thrown if the input stream ends and the number of available bytes
   *     is less than numBytes
   * @throw IOException thrown if any other I/O errors occur
   */
  public String readString(int numBytes) throws NotEnoughDataException, IOException {
    if (numBytes == 0) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    while (numBytes > 0) {
      int availableConsecutiveLength =
          writeOffset >= readOffset ? writeOffset - readOffset : bufferSize - readOffset;
      if (availableConsecutiveLength > 0) {
        int numRead = Math.min(availableConsecutiveLength, numBytes);
        String s = new String(buffer, readOffset, numRead, UTF_8);
        readOffset = incrementOffset(readOffset, numRead);
        numBytes -= numRead;
        sb.append(s);
      } else {
        if (!fillBuffer()) {
          throw new NotEnoughDataException("Input stream ended without enough data.");
        }
      }
    }
    return sb.toString();
  }

  @Override
  public void close() throws IOException {
    inputStream.close();
  }

  /**
   * Read more data from the input stream to the buffer.
   *
   * @return whether at least one byte of data is read. If it's false, it means the stream has ended
   *     and no more data will be available
   */
  boolean fillBuffer() throws IOException {
    logger.fine("fillBuffer: readOffset: %d, writeOffset: %d", readOffset, writeOffset);
    int remainCapacity = getRemainCapacity();
    if (remainCapacity == 0) {
      return false;
    }
    // Either readOffset > writeOffset or readOffset == 0, so the writeable buffer is consecutive
    // and its size is the remaining capacity; or the consecutive buffer is from the write offset
    // to the end of the buffer.
    int numToRead = Math.min(getRemainCapacity(), bufferSize - writeOffset);
    int numRead = inputStream.read(buffer, writeOffset, numToRead);
    if (numRead > 0) {
      writeOffset = incrementOffset(writeOffset, numRead);
      logger.fine("fillBuffer: %d bytes read, writeOffset: %d", readOffset, writeOffset);
      return true;
    }
    return false;
  }

  /** @returns how many more bytes can be read from the input stream */
  private int getRemainCapacity() {
    int remainCapacity = readOffset - 1 - writeOffset;
    if (remainCapacity < 0) {
      remainCapacity += bufferSize;
    }
    return remainCapacity;
  }

  /**
   * Increment {@code currentOffset} by {@code delta}. Rewind the incremented value if exceeds
   * buffer size.
   *
   * @param currentValue the current offset value, must be in the range of [0, bufferSize)
   * @param delta the amount to increment, must be in the range of [0, bufferSize)
   */
  private int incrementOffset(int currentOffset, int delta) {
    int ret = currentOffset + delta;
    if (ret >= bufferSize) {
      ret -= bufferSize;
    }
    return ret;
  }
}
