package org.javacomp.protocol.javacomp;

import java.util.List;
import javax.annotation.Nullable;
import org.javacomp.protocol.TextEdit;

/** Internal representation of a document's content. */
public class DocumentContent {
  /**
   * Whether the document is opened by the client or not.
   *
   * <p>If the document is opened by the client, {@link #editHistory} is non-null and {@link
   * #snapshotContent} is the end result of applying {@link #editHistory}. If the document is not
   * opened by the client, {@link #editHistory} is null and {@link #snapshotContent} is the file
   * content stored in the filesystem.
   */
  public boolean openedByClient;

  /** The file content JavaComp is using for parsing and completion. */
  public String snapshotContent;

  /**
   * The result of applying {@link org.javacomp.parser.FileContentFixer} on {@link
   * #snapshotContent}.
   */
  public String fixedContent;

  /** The history of editing if the document is opened by client. */
  @Nullable public EditHistory editHistory;

  public static class EditHistory {
    /** The snapshot of the document content when editing starts. */
    public String orignalContent;

    /** The text edits after original content. */
    public List<TextEdit> textEdits;
  }
}
