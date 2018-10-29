package org.javacomp.server.handler.javacomp;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableList;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.javacomp.file.EditHistory;
import org.javacomp.file.FileManager;
import org.javacomp.parser.FileContentFixer;
import org.javacomp.parser.ParserContext;
import org.javacomp.protocol.javacomp.DocumentContent;
import org.javacomp.protocol.javacomp.DocumentContentParams;
import org.javacomp.server.Request;
import org.javacomp.server.Server;
import org.javacomp.server.handler.RequestHandler;
import org.javacomp.server.handler.utils.MessageUtils;

/** Handles "$JavaComp/documentContent" request. */
public class DocumentContentHandler extends RequestHandler<DocumentContentParams> {
  private final Server server;

  public DocumentContentHandler(Server server) {
    super("$JavaComp/documentContent", DocumentContentParams.class);
    this.server = server;
  }

  @Override
  public DocumentContent handleRequest(Request<DocumentContentParams> request) throws Exception {
    URI uri = request.getParams().uri;
    Path path = Paths.get(uri);
    FileManager fileManager = server.getFileManager();
    FileContentFixer fixer = new FileContentFixer(new ParserContext());
    Optional<EditHistory> editHistory = fileManager.getFileEditHistory(path);
    Optional<CharSequence> content = fileManager.getFileContent(path);
    checkArgument(content.isPresent(), "File %s does not exist", uri);
    DocumentContent ret = new DocumentContent();
    ret.openedByClient = editHistory.isPresent();
    ret.snapshotContent = content.get().toString();
    ret.fixedContent = fixer.fixFileContent(ret.snapshotContent).getContent();
    if (editHistory.isPresent()) {
      ret.editHistory = new DocumentContent.EditHistory();
      ret.editHistory.orignalContent = editHistory.get().getOriginalContent();
      ret.editHistory.textEdits =
          editHistory
              .get()
              .getAppliedEdits()
              .stream()
              .map(MessageUtils::buildTextEdit)
              .collect(ImmutableList.toImmutableList());
    }
    return ret;
  }
}
