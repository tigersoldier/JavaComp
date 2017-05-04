package org.javacomp.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.parser.Scanner;
import com.sun.tools.javac.parser.Tokens.Token;
import com.sun.tools.javac.parser.Tokens.TokenKind;
import com.sun.tools.javac.util.Position.LineMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Correct the content of a Java file for completion. */
public class FileContentFixer {
  private static final Set<TokenKind> VALID_MEMBER_SELECTION_TOKENS =
      ImmutableSet.of(TokenKind.IDENTIFIER, TokenKind.LT, TokenKind.NEW);
  private final ParserContext parserContext;

  public FileContentFixer(ParserContext parserContext) {
    this.parserContext = parserContext;
  }

  public FixedContent fixFileContent(CharSequence content) {
    Scanner scanner = parserContext.tokenize(content, false /* keepDocComments */);
    List<Insertion> insertions = new ArrayList<>();
    for (; ; scanner.nextToken()) {
      Token token = scanner.token();
      if (token.kind == TokenKind.EOF) {
        break;
      }
      if (token.kind == TokenKind.DOT) {
        fixMemberSelection(scanner, insertions);
      }
    }
    CharSequence modifiedContent = Insertion.applyInsertions(content, insertions);
    return FixedContent.create(
        modifiedContent, createAdjustedLineMap(scanner.getLineMap(), insertions));
  }

  private void fixMemberSelection(Scanner scanner, List<Insertion> insertions) {
    Token token = scanner.token();
    Token nextToken = scanner.token(1);

    LineMap lineMap = scanner.getLineMap();
    int tokenLine = lineMap.getLineNumber(token.pos);
    int nextLine = lineMap.getLineNumber(nextToken.pos);

    if (nextLine > tokenLine) {
      // The line ends with a dot. It's likely the user is entering a dot and waiting for member
      // completion. The current line is incomplete and syntextually invalid.
      insertions.add(Insertion.create(token.endPos, "dumbIdent;"));
    } else if (!VALID_MEMBER_SELECTION_TOKENS.contains(nextToken.kind)) {
      // The member selection is syntextually invalid. Fix it.
      insertions.add(Insertion.create(token.endPos, "dumbIdent;"));
    }
  }

  private AdjustedLineMap createAdjustedLineMap(
      LineMap originalLineMap, List<Insertion> insertions) {
    return new AdjustedLineMap.Builder()
        .setOriginalLineMap(originalLineMap)
        .addInsertions(insertions)
        .build();
  }

  @AutoValue
  public abstract static class FixedContent {
    public abstract String getContent();

    public abstract com.sun.source.tree.LineMap getAdjustedLineMap();

    public static FixedContent create(CharSequence content, AdjustedLineMap lineMap) {
      return new AutoValue_FileContentFixer_FixedContent(content.toString(), lineMap);
    }
  }
}
