package test.data;

public class TestData {
  private final int a;

  private TestData() {
    a = 0;
  }

  public TestData(int intParam, String strParam) {
    a = intParam;
  }

  public boolean publicIfBlockMethod() {
    if (a == 1) {
      return true;
    } else {
      return false;
    }
  }

  protected int protectedWhileBlockMethod(int number) {
    int ret = 0;
    while (number > 0) {
      number--;
      ret++;
    }
    return ret;
  }

  private String privateForBlockMethod(List<String> input) {
    String output;
    for (String s : input) {
      output += s;
    }
    for (int i = 0; i < input.size(); i++) {
      output += input.get(i);
    }
    return output;
  }

  void packagePrivateSwitchCaseMethod() {
    switch (a) {
      case 0:
        int switchScopeVar = a;
        a = 1;
        break;
      case 1:
        {
          int caseScopeVar = a;
          a = 0;
          break;
        }
      default:
        a = 2;
    }
  }

  private static class PrivateStaticSubClass {
    private void privateMethod1() {}

    public void publicMethod1() {}
  }

  public class PublicSubClass {
    public void publicMethod2() {}

    private void privateMethod2() {}
  }
}
