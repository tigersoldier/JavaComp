package test.data;

import foo.Bar.Baz;
import foo.bar.*;
import foo.bar.baz.*;

public class TestData {
  public static final int publicStaticIntField;
  private final boolean privateMemberBooleanField;

  private TestData() {
    a = 0;
  } // TestData constructor w/o params

  public TestData(int intParam, String strParam) {
    a = intParam;
  } // TestData constructor w/ 2 params

  public boolean publicIfBlockMethod() {
    Baz methodScopeVar;
    if (a == 1) {
      int ifScopeVar;
      return true;
    } else { // end of if
      int elseScopeVar;
      return false;
    } // else
  } // publicIfBlockMethod

  protected int protectedWhileBlockMethod(int number) {
    int ret = 0;
    while (number > 0) {
      int whileScopeVar;
      number--;
      ret++;
    } // while loop
    return ret;
  } // protectedWhileBlockMethod

  private String privateForBlockMethod(java.util.List<String> input) {
    String output;
    for (String s : input) {
      int forScopeVar;
      output += s;
    } // for loop
    for (int i = 0; i < input.size(); i++) {
      output += input.get(i);
    }
    return output;
  } // privateForBlockMethod

  private void annonymousClass() {
    this.setInnerInterfaceObject(
        new PublicInnerInterface() {
          @Override
          public void interfaceMethod() {
            // noop
          }

          private void privateAnnonymousClassMethod() {}
        } /* end of new PublicInnerInterface */);
  }

  void packagePrivateSwitchCaseMethod() {
    switch (a) {
      case 0:
        int switchScopeVar = a;
        a = 1;
        break;
      case 1:
        { // start of case block
          int caseScopeVar = a;
          a = 0;
          break;
        } // end of case block
      default:
        a = 2;
    } // switch
  } // packagePrivateSwitchCaseMethod

  private static class PrivateStaticInnerClass {
    private void privateMethod1() {}

    public void publicMethod1() {}
  } // PrivateStaticInnerClass

  public class PublicInnerClass {
    public void publicMethod2() {}

    private void privateMethod2() {}
  } // PublicInnerClass

  public interface PublicInnerInterface {
    void interfaceMethod();
  } // PublicInnerInterface

  public @interface PublicInnerAnnotation {
    int value();
  } // PublicInnerAnnotation

  public enum PublicInnerEnum {
    ENUM_VALUE1,
    ENUM_VALUE2,
    ;

    public void publicEnumMethod() {}
  } // PublicInnerEnum
} // class TestData
