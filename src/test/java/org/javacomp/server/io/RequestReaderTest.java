package org.javacomp.server.io;

import static com.google.common.truth.Truth.assertThat;

import java.io.StringBufferInputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class RequestReaderTest {
  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void testReadLines() throws Exception {
    RequestReader reader =
        createReader(
            "1. Line ends with CRLF\r\n"
                + "2. Line ends with CR\r"
                + "3. Line ends with LF\n"
                + "\n\n\r\r\r\n"
                + "4. Last line\n",
            30 /* capacity */);
    assertThat(reader.readLine()).isEqualTo("1. Line ends with CRLF");
    assertThat(reader.readLine()).isEqualTo("2. Line ends with CR");
    assertThat(reader.readLine()).isEqualTo("3. Line ends with LF");
    assertThat(reader.readLine()).isEqualTo(""); // \n
    assertThat(reader.readLine()).isEqualTo(""); // \n
    assertThat(reader.readLine()).isEqualTo(""); // \r
    assertThat(reader.readLine()).isEqualTo(""); // \r
    assertThat(reader.readLine()).isEqualTo(""); // \r\n
    assertThat(reader.readLine()).isEqualTo("4. Last line");
  }

  @Test
  public void testReadLongLines() throws Exception {
    RequestReader reader =
        createReader("1234\r\n5678\r9012\n12345\r\n123456\r\n", 5 /* capacity */);
    assertThat(reader.readLine()).isEqualTo("1234");
    assertThat(reader.readLine()).isEqualTo("5678");
    assertThat(reader.readLine()).isEqualTo("9012");
    assertThat(reader.readLine()).isEqualTo("12345");
    assertThat(reader.readLine()).isEqualTo("123456");
  }

  @Test
  public void testReadLine_noCrLf_throwsNotEnoughDataException() throws Exception {
    RequestReader reader = createReader("12345", 50);
    thrown.expect(NotEnoughDataException.class);
    reader.readLine();
  }

  @Test
  public void testReadString() throws Exception {
    RequestReader reader =
        createReader("0123456789A123456789B123456789C123456789D123", 9 /* capacity */);

    // readOffset: 0, writeOffset: 0
    assertThat(reader.readString(1)).isEqualTo("0");
    // buffer filled, readOffset: 1, writeOffset: 9
    assertThat(reader.readString(4)).isEqualTo("1234");
    // buffer not filled, readOffset: 5, writeOffset: 9
    reader.fillBuffer();
    // buffer filled, readOffset: 5, writeOffset: 4
    assertThat(reader.readString(1)).isEqualTo("5");
    // buffer not filled, readOffset: 6, writeOffset: 4
    reader.fillBuffer();
    // buffer filled, readOffset: 6, writeOffset: 5
    assertThat(reader.readString(8)).isEqualTo("6789A123");
    // buffer not filled, readOffset: 4, writeOffset: 5
    assertThat(reader.readString(6)).isEqualTo("456789");
    // buffer not filled, readOffset: 0, writeOffset: 0
    assertThat(reader.readString(5)).isEqualTo("B1234");
    // buffer filled, readOffset: 5, writeOffset: 9
    assertThat(reader.readString(10)).isEqualTo("56789C1234");
    // buffer filled, readOffset: 5, writeOffset: 9
    assertThat(reader.readString(9)).isEqualTo("56789D123");
  }

  @Test
  public void testReadString_numBytesGreaterThanCapacity() throws Exception {
    RequestReader reader =
        createReader("0123456789A123456789B123456789C123456789D123", 5 /* capacity */);

    assertThat(reader.readString(10)).isEqualTo("0123456789");
    assertThat(reader.readString(15)).isEqualTo("A123456789B1234");
    assertThat(reader.readString(19)).isEqualTo("56789C123456789D123");
  }

  @Test
  public void testReadString_notEnoughData_throwsException() throws Exception {
    RequestReader reader = createReader("0123456789", 20 /* capacity */);

    thrown.expect(NotEnoughDataException.class);
    reader.readString(11);
  }

  private RequestReader createReader(String input, int capacity) {
    StringBufferInputStream in = new StringBufferInputStream(input);
    return new RequestReader(in, capacity);
  }
}
