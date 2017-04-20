package org.javacomp.server.protocol;

import com.google.gson.JsonElement;
import javax.annotation.Nullable;

/**
 * Represents a reference to a command. Provides a title which will be used to represent a command
 * in the UI. Commands are identitifed using a string identifier and the protocol currently doesn't
 * specify a set of well known commands. So executing a command requires some tool extension code.
 */
public class Command {
  /** Title of the command, like `save`. */
  public String title;
  /** The identifier of the actual command handler. */
  public String command;
  /** Arguments that the command handler should be invoked with. */
  @Nullable public JsonElement[] arguments;
}
