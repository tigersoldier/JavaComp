package org.javacomp.server.protocol;

import com.google.gson.JsonElement;
import javax.annotation.Nullable;
/**
 * The result returned by "initialize" method.
 *
 * <p>See
 * https://github.com/Microsoft/language-server-protocol/blob/master/protocol.md#initialize-request
 */
public class InitializeResult {
  /** The capabilities the language server provides. */
  public ServerCapabilities capabilities;

  /** Defines how the host (editor) should sync document changes to the language server. */

  /** Completion options. */
  public static class CompletionOptions {
    /** The server provides support to resolve additional information for a completion item. */
    @Nullable public boolean resolveProvider;
    /** The characters that trigger completion automatically. */
    @Nullable public String[] triggerCharacters;
  }

  /** Signature help options. */
  public static class SignatureHelpOptions {
    /** The characters that trigger signature help automatically. */
    @Nullable public String[] triggerCharacters;
  }

  /** Code Lens options. */
  public static class CodeLensOptions {
    /** Code lens has a resolve provider as well. */
    @Nullable public boolean resolveProvider;
  }

  /** Format document on type options */
  public static class DocumentOnTypeFormattingOptions {
    /** A character on which formatting should be triggered, like `}`. */
    public String firstTriggerCharacter;

    /** More trigger characters. */
    @Nullable public String[] moreTriggerCharacter;
  }

  /** Document link options */
  public static class DocumentLinkOptions {
    /** Document links have a resolve provider as well. */
    @Nullable public boolean resolveProvider;
  }

  /** Execute command options. */
  public static class ExecuteCommandOptions {
    /** The commands to be executed on the server */
    public String[] commands;
  }

  /** Save options. */
  public static class SaveOptions {
    /** The client is supposed to include the content on save. */
    @Nullable public boolean includeText;
  }

  public enum TextDocumentSyncKind {
    /** Documents should not be synced at all. */
    NONE,

    /** Documents are synced by always sending the full content of the document. */
    FULL,

    /**
     * Documents are synced by sending the full content on open. After that only incremental updates
     * to the document are send.
     */
    INCREMENTAL,
  }

  public static class TextDocumentSyncOptions {
    /** Open and close notifications are sent to the server. */
    @Nullable public boolean openClose;
    /** Change notificatins are sent to the server. */
    @Nullable public TextDocumentSyncKind change;
    /** Will save notifications are sent to the server. */
    @Nullable public boolean willSave;
    /** Will save wait until requests are sent to the server. */
    @Nullable public boolean willSaveWaitUntil;
    /** Save notifications are sent to the server. */
    @Nullable public SaveOptions save;
  }

  public static class ServerCapabilities {
    /** Defines how text documents are synced. */
    @Nullable public TextDocumentSyncOptions textDocumentSync;
    /** The server provides hover support. */
    @Nullable public boolean hoverProvider;
    /** The server provides completion support. */
    @Nullable public CompletionOptions completionProvider;
    /** The server provides signature help support. */
    @Nullable public SignatureHelpOptions signatureHelpProvider;
    /** The server provides goto definition support. */
    @Nullable public boolean definitionProvider;
    /** The server provides find references support. */
    @Nullable public boolean referencesProvider;
    /** The server provides document highlight support. */
    @Nullable public boolean documentHighlightProvider;
    /** The server provides document symbol support. */
    @Nullable public boolean documentSymbolProvider;
    /** The server provides workspace symbol support. */
    @Nullable public boolean workspaceSymbolProvider;
    /** The server provides code actions. */
    @Nullable public boolean codeActionProvider;
    /** The server provides code lens. */
    @Nullable public CodeLensOptions codeLensProvider;
    /** The server provides document formatting. */
    @Nullable public boolean documentFormattingProvider;
    /** The server provides document range formatting. */
    @Nullable public boolean documentRangeFormattingProvider;
    /** The server provides document formatting on typing. */
    @Nullable public DocumentOnTypeFormattingOptions documentOnTypeFormattingProvider;
    /** The server provides rename support. */
    @Nullable public boolean renameProvider;
    /** The server provides document link support. */
    @Nullable public DocumentLinkOptions documentLinkProvider;
    /** The server provides execute command support. */
    @Nullable public ExecuteCommandOptions executeCommandProvider;
    /** Experimental server capabilities. */
    @Nullable public JsonElement experimental;
  }
}
