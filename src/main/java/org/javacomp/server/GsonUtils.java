package org.javacomp.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Utilities for creating Gson for JavaComp. */
public class GsonUtils {
  private static final Gson GSON_INSTANCE =
      new GsonBuilder().registerTypeAdapterFactory(new OrdinalEnumTypeAdapterFactory()).create();

  public static Gson getGson() {
    return GSON_INSTANCE;
  }

  /** A TypeAdapterFactory that converts between enum values and their ordinal numbers. */
  public static class OrdinalEnumTypeAdapterFactory implements TypeAdapterFactory {
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
      @SuppressWarnings("unchecked")
      Class<T> rawType = (Class<T>) type.getRawType();
      if (!rawType.isEnum()) {
        return null;
      }

      final Map<Integer, T> ordinalToConstant = new HashMap<>();
      for (T constant : rawType.getEnumConstants()) {
        @SuppressWarnings("unchecked")
        Enum enumConstant = (Enum) constant;
        ordinalToConstant.put(enumConstant.ordinal(), constant);
      }

      return new TypeAdapter<T>() {
        public void write(JsonWriter out, T value) throws IOException {
          if (value == null) {
            out.nullValue();
          } else {
            out.value(((Enum) value).ordinal());
          }
        }

        public T read(JsonReader reader) throws IOException {
          if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
          } else {
            return ordinalToConstant.get(reader.nextInt());
          }
        }
      };
    }
  }
}
