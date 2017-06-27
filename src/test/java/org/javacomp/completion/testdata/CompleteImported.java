package org.javacomp.completion.testdata;

import static org.javacomp.completion.testdata.CompleteImported.ExplicitStatic.staticField;
import static org.javacomp.completion.testdata.CompleteImported.ExplicitStatic.staticMethid;
import static org.javacomp.completion.testdata.CompleteImported.OnDemandStatic.*;

import org.javacomp.completion.testdata.CompleteImported.ExplicitClass.ExplicitInnerClass;
import org.javacomp.completion.testdata.CompleteImported.OnDemandClass.*;

public class CompleteImported {

  public static class ExplicitStatic {
    public static int staticField;
    public static void staticMethid();
  }

  public static class ExplicitClass {
    public static class ExplicitInnerClass {
    }
  }

  public static class OnDemandStatic {
    public static int onDemandStaticField;
    public static void onDemandStaticMethod();
  }

  public static class OnDemandClass {
    public static class OnDemandInnerClass {
    }
  }

  public void testMethod() {
    /** @complete */
  }
}
