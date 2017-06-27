package org.javacomp.typesolver.testdata.ondemand;

public class OnDemand {
  public static final InnerOnDemand STATIC_ON_DEMAND_FIELD = 0;
  public static final InnerOnDemand staticOnDemandMethod() {}
  public static final OnDemand staticOnDemandMethod(int v) {}
  public class InnerOnDemand {}
}
