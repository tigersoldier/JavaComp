package org.javacomp.server;

/** Constants of the server. */
final class Constants {
  // Version numbers of JavaComp. We use Semantic Versioning 2.0.0.
  // See http://semver.org/

  /** Major version. Only increment it when making backwards-incompatible changes. */
  public static final int MAJOR_VERSION = 1;
  /**
   * Minor version. Increment it when adding new functionalities in a backwards-compatible manner.
   */
  public static final int MINOR_VERSION = 0;
  /** Patch version. Increment it when making backwards-compatible bug fixes. */
  public static final int PATCH_VERSION = 0;

  private Constants() {}
}
