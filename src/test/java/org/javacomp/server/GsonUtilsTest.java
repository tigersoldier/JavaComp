package org.javacomp.server;

import static com.google.common.truth.Truth.assertThat;

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
  }

  @Test
  public void testDeserializesOrdinalToEnum() {
    assertThat(gson.fromJson("0", TestEnum.class)).isEqualTo(TestEnum.ZERO);
    assertThat(gson.fromJson("1", TestEnum.class)).isEqualTo(TestEnum.ONE);
    assertThat(gson.fromJson("2", TestEnum.class)).isEqualTo(TestEnum.TWO);
  }

  private enum TestEnum {
    ZERO,
    ONE,
    TWO,
  }
}
