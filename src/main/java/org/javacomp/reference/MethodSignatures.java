package org.javacomp.reference;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.javacomp.model.MethodEntity;

/** Information about all overloads of a method. */
@AutoValue
public abstract class MethodSignatures {
  /** All methods. The first element is the best matched method, if not empty. */
  public abstract List<MethodEntity> getMethods();

  /**
   * The 0-based index of the parameter of the first method in the return value {@link #getMethods}.
   * If {@link #getMethods} returns an empty list, or the first method it returns has no parameter,
   * the value is 0 and should be ignored.
   */
  public abstract int getActiveParameter();

  public static MethodSignatures create(List<MethodEntity> methods, int activeParameter) {
    return new AutoValue_MethodSignatures(ImmutableList.copyOf(methods), activeParameter);
  }
}
