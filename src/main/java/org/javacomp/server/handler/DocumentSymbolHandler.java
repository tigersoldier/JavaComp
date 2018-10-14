package org.javacomp.server.handler;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nullable;
import org.javacomp.logging.JLogger;
import org.javacomp.model.ClassEntity;
import org.javacomp.model.Entity;
import org.javacomp.model.EntityScope;
import org.javacomp.model.FileScope;
import org.javacomp.project.FileItem;
import org.javacomp.protocol.ClientCapabilities;
import org.javacomp.protocol.ClientCapabilities.DocumentSymbolCapabilities;
import org.javacomp.protocol.DocumentSymbol;
import org.javacomp.protocol.DocumentSymbolInformation;
import org.javacomp.protocol.DocumentSymbolParams;
import org.javacomp.protocol.SymbolInformation;
import org.javacomp.protocol.SymbolKind;
import org.javacomp.server.Request;
import org.javacomp.server.Server;

/**
 * Handles "textDocument/hover" notification.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#textDocument_hover
 */
public class DocumentSymbolHandler extends RequestHandler<DocumentSymbolParams> {
  private static final JLogger logger = JLogger.createForEnclosingClass();

  private static final ImmutableSet<SymbolKind> DEFAULT_SUPPORTED_SYMBOL_KINDS =
      ImmutableSet.of(
          SymbolKind.FILE,
          SymbolKind.MODULE,
          SymbolKind.NAMESPACE,
          SymbolKind.PACKAGE,
          SymbolKind.CLASS,
          SymbolKind.METHOD,
          SymbolKind.PROPERTY,
          SymbolKind.FIELD,
          SymbolKind.CONSTRUCTOR,
          SymbolKind.ENUM,
          SymbolKind.INTERFACE,
          SymbolKind.FUNCTION,
          SymbolKind.VARIABLE,
          SymbolKind.CONSTANT,
          SymbolKind.STRING,
          SymbolKind.NUMBER,
          SymbolKind.BOOLEAN,
          SymbolKind.ARRAY);

  private final Server server;

  public DocumentSymbolHandler(Server server) {
    super("textDocument/documentSymbol", DocumentSymbolParams.class);
    this.server = server;
  }

  @Override
  public List<? extends DocumentSymbolInformation> handleRequest(
      Request<DocumentSymbolParams> request) throws Exception {
    Optional<FileItem> fileItem =
        server.getProject().getFileItem(Paths.get(request.getParams().textDocument.uri));
    if (!fileItem.isPresent()) {
      return ImmutableList.of();
    }

    ClientCapabilities clientCapabilities = server.getClientCapabilities();
    DocumentSymbolCapabilities documentSymbolCapabilities = null;
    Set<SymbolKind> supportedSymbolKinds = DEFAULT_SUPPORTED_SYMBOL_KINDS;
    boolean supportDocumentSymbol = false;
    if (clientCapabilities.textDocument != null) {
      documentSymbolCapabilities = clientCapabilities.textDocument.documentSymbol;
    }
    if (documentSymbolCapabilities != null) {
      supportDocumentSymbol = documentSymbolCapabilities.hierarchicalDocumentSymbolSupport;
      if (documentSymbolCapabilities.symbolKind != null
          && documentSymbolCapabilities.symbolKind.valueSet != null) {
        supportedSymbolKinds = documentSymbolCapabilities.symbolKind.valueSet;
      }
    }

    FileScope fileScope = fileItem.get().getFileScope();
    if (supportDocumentSymbol) {
      ImmutableList.Builder<DocumentSymbol> symbols = new ImmutableList.Builder<>();
      addDocumentSymbolsInScope(symbols, fileScope, fileScope, supportedSymbolKinds);
      return symbols.build();
    } else {
      ImmutableList.Builder<SymbolInformation> symbols = new ImmutableList.Builder<>();
      addSymbolInformationsInScope(
          symbols,
          /* scope= */ fileScope,
          /* fileScope= */ fileScope,
          supportedSymbolKinds,
          /* containerName1= */ null);
      return symbols.build();
    }
  }

  private void addDocumentSymbolsInScope(
      ImmutableList.Builder<DocumentSymbol> symbols,
      EntityScope scope,
      FileScope fileScope,
      Set<SymbolKind> supportedSymbolKinds) {
    logger.fine("Building document symbols for scope %s", scope);

    @Nullable DocumentSymbol newSymbol = null;
    ImmutableList.Builder<DocumentSymbol> children = symbols;
    Optional<Entity> entity = scope.getDefiningEntity();
    @Nullable
    SymbolKind symbolKind =
        entity.isPresent() ? getSymbolKind(entity.get(), supportedSymbolKinds) : null;
    if (symbolKind != null) {
      newSymbol = new DocumentSymbol();
      newSymbol.name = entity.get().getSimpleName();
      newSymbol.kind = symbolKind;
      newSymbol.selectionRange =
          MessageUtils.buildRangeForFile(fileScope, entity.get().getSymbolRange());
      newSymbol.range =
          MessageUtils.buildRangeForFile(fileScope, entity.get().getScope().getDefinitionRange());
      symbols.add(newSymbol);
      children = new ImmutableList.Builder<>();
    }
    for (EntityScope childScope : scope.getChildScopes()) {
      addDocumentSymbolsInScope(children, childScope, fileScope, supportedSymbolKinds);
    }
    if (newSymbol != null) {
      newSymbol.children = children.build();
    }
  }

  private void addSymbolInformationsInScope(
      ImmutableList.Builder<SymbolInformation> symbols,
      EntityScope scope,
      FileScope fileScope,
      Set<SymbolKind> supportedSymbolKinds,
      @Nullable String containerName) {
    logger.fine("Building symbol informations in scope %s", scope);
    for (Entity entity : scope.getMemberEntities().values()) {
      SymbolKind symbolKind = getSymbolKind(entity, supportedSymbolKinds);
      if (symbolKind != null) {
        SymbolInformation symbol = new SymbolInformation();
        symbol.name = entity.getSimpleName();
        symbol.kind = symbolKind;
        symbol.location = MessageUtils.buildLocationForFile(fileScope, entity.getSymbolRange());
        symbol.containerName = containerName;
        symbols.add(symbol);
        // The entity has enclosing scopes. This is false for VariableEntity.
        String newContainerName =
            (containerName == null) ? symbol.name : containerName + "." + symbol.name;
        addSymbolInformationsInScope(
            symbols,
            entity.getScope(),
            fileScope,
            supportedSymbolKinds,
            /* containerName= */ symbol.name);
      }
    }
  }

  @Nullable
  private SymbolKind getSymbolKind(Entity entity, Set<SymbolKind> supportedSymbolKinds) {
    SymbolKind symbolKind = null;
    Optional<Entity> parentEntity =
        entity.getParentScope().map(scope -> scope.getDefiningEntity().orElse(null));
    switch (entity.getKind()) {
      case INTERFACE:
        symbolKind = SymbolKind.INTERFACE;
        break;
      case CLASS:
        symbolKind = SymbolKind.CLASS;
        break;
      case ANNOTATION:
        symbolKind = SymbolKind.INTERFACE;
        break;
      case ENUM:
        symbolKind = SymbolKind.ENUM;
        break;
      case FIELD:
        symbolKind = SymbolKind.FIELD;
        if (supportedSymbolKinds.contains(SymbolKind.ENUM_MEMBER)
            && parentEntity.isPresent()
            && parentEntity.get().getKind() == Entity.Kind.ENUM) {
          symbolKind = SymbolKind.ENUM_MEMBER;
        }
        break;
      case METHOD:
        if ((parentEntity.isPresent() && parentEntity.get() instanceof ClassEntity)
            && (parentEntity.get().getSimpleName().equals("<init>"))) {
          symbolKind = SymbolKind.CONSTRUCTOR;
        } else {
          symbolKind = SymbolKind.METHOD;
        }
        break;
      case VARIABLE:
        symbolKind = SymbolKind.VARIABLE;
        break;
      default:
        logger.fine("", entity.getKind());
    }
    logger.fine("Symbol %s for %s", symbolKind, entity);
    return symbolKind;
  }
}
