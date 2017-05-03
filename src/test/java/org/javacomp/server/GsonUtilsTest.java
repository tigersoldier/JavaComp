package org.javacomp.server;

import static com.google.common.truth.Truth.assertThat;
import static org.javacomp.server.GsonEnum.SerializeType.LOWERCASE_NAME;
import static org.javacomp.server.GsonEnum.SerializeType.ORDINAL;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class GsonUtilsTest {
  private final Gson gson = GsonUtils.getGson();

  @Test
  public void testSerializesEnumToOrdinal() {
    assertThat(gson.toJson(TestEnum.ZERO)).isEqualTo("0");
    assertThat(gson.toJson(TestEnum.ONE)).isEqualTo("1");
    assertThat(gson.toJson(TestEnum.TWO)).isEqualTo("2");
    assertThat(gson.toJson(OrdinalEnum.ZERO)).isEqualTo("0");
    assertThat(gson.toJson(OrdinalEnum.ONE)).isEqualTo("1");
    assertThat(gson.toJson(OrdinalEnum.TWO)).isEqualTo("2");
  }

  @Test
  public void testDeserializesOrdinalToEnum() {
    assertThat(gson.fromJson("0", TestEnum.class)).isEqualTo(TestEnum.ZERO);
    assertThat(gson.fromJson("1", TestEnum.class)).isEqualTo(TestEnum.ONE);
    assertThat(gson.fromJson("2", TestEnum.class)).isEqualTo(TestEnum.TWO);
    assertThat(gson.fromJson("0", OrdinalEnum.class)).isEqualTo(OrdinalEnum.ZERO);
    assertThat(gson.fromJson("1", OrdinalEnum.class)).isEqualTo(OrdinalEnum.ONE);
    assertThat(gson.fromJson("2", OrdinalEnum.class)).isEqualTo(OrdinalEnum.TWO);
  }

  @Test
  public void testSerializesEnumToLowercasedName() {
    assertThat(gson.toJson(LowercaseEnum.ZERO)).isEqualTo("\"zero\"");
    assertThat(gson.toJson(LowercaseEnum.ONE)).isEqualTo("\"one\"");
    assertThat(gson.toJson(LowercaseEnum.TWO)).isEqualTo("\"two\"");
  }

  @Test
  public void testDeserializesLowercasedNameToEnum() {
    assertThat(gson.fromJson("zero", LowercaseEnum.class)).isEqualTo(LowercaseEnum.ZERO);
    assertThat(gson.fromJson("one", LowercaseEnum.class)).isEqualTo(LowercaseEnum.ONE);
    assertThat(gson.fromJson("two", LowercaseEnum.class)).isEqualTo(LowercaseEnum.TWO);
  }

  private enum TestEnum {
    ZERO,
    ONE,
    TWO,
  }

  @GsonEnum(ORDINAL)
  private enum OrdinalEnum {
    ZERO,
    ONE,
    TWO,
  }

  @GsonEnum(LOWERCASE_NAME)
  private enum LowercaseEnum {
    ZERO,
    ONE,
    TWO,
  }
}
