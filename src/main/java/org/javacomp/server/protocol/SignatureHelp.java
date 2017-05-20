package org.javacomp.server.protocol;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Signature help represents the signature of something callable. There can be multiple signature
 * but only one active and only one active parameter.
 */
public class SignatureHelp {
  /** One or more signatures. */
  public List<SignatureInformation> signatures;

  /**
   * The active signature.
   *
   * <p>If omitted or the value lies outside the range of `signatures` the value defaults to zero or
   * is ignored if `signatures.length === 0`. Whenever possible implementors should make an active
   * decision about the active signature and shouldn't rely on a default value.
   *
   * <p>In future version of the protocol this property might become mandantory to better express
   * this.
   */
  @Nullable public int activeSignature;

  /**
   * The active parameter of the active signature.
   *
   * <p>If omitted or the value lies outside the range of {@code
   * signatures[activeSignature].parameters} defaults to 0 if the active signature has parameters.
   * If the active signature has no parameters it is ignored.
   *
   * <p>In future version of the protocol this property might become mandantory to better express
   * the active parameter if the active signature does have any.
   */
  @Nullable public int activeParameter;

  /**
   * Represents the signature of something callable. A signature can have a label, like a
   * function-name, a doc-comment, and a set of parameters.
   */
  public static class SignatureInformation {
    /** The label of this signature. Will be shown in the UI. */
    public String label;

    /**
     * The human-readable doc-comment of this signature. Will be shown in the UI but can be omitted.
     */
    @Nullable public String documentation;

    /** The parameters of this signature. */
    @Nullable public List<ParameterInformation> parameters;
  }

  /**
   * Represents a parameter of a callable-signature. A parameter can have a label and a doc-comment.
   */
  public static class ParameterInformation {
    /** The label of this parameter. Will be shown in the UI. */
    public String label;

    /**
     * The human-readable doc-comment of this parameter. Will be shown in the UI but can be omitted.
     */
    @Nullable public String documentation;
  }
}
