package test.data;

import java.util.AbstractList;
import java.util.List;
import java.util.Map;

public class TestClass extends AbstractList<String> implements Comparable<TestClass> {
  public static final int PUBLIC_STATIC_FIELD = 0;
  private final List<String> privateStringListField = null;
  protected String protectedStringField;
  boolean packagePrivateBooleanField;
  private InnerClass<String>.InnerClass2.InnerClass3 innerClass3Field;

  @Override
  public int compareTo(TestClass other) {
    return 0;
  }

  @Override
  public String get(int index) {
    return null;
  }

  @Override
  public int size() {
    return 0;
  }

  public interface InnerInterface {
    <X extends Exception, Y> X genericMethod(
        List<X> arg1, String arg2, Map.Entry<? extends X, ? super Y> arg3, List<?> arg4)
        throws X, Exception;
  }

  public class InnerClass<T> {
    public class InnerClass2 {
      public class InnerClass3 {
        T tField;
      }
    }
  }

  public enum InnerEnum {
    ENUM_FIELD,
  }

  public @interface InnerAnnotation {
  }
}
