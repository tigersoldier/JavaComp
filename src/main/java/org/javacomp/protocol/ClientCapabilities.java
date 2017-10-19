package org.javacomp.protocol;

import com.google.gson.JsonElement;
import javax.annotation.Nullable;

/** Client capability submessage. */
public class ClientCapabilities {
  /** Workspace specific client capabilities. */
  @Nullable public WorkspaceClientCapabilites workspace;

  /** Text document specific client capabilities. */
  @Nullable public TextDocumentClientCapabilities textDocument;

  /** Experimental client capabilities. */
  @Nullable public JsonElement experimental;

  public static class WorkspaceClientCapabilites {
    /**
     * The client supports applying batch edits to the workspace by supporting the request
     * 'workspace/applyEdit'
     */
    @Nullable public boolean applyEdit;

    /** Capabilities specific to `WorkspaceEdit`s */
    @Nullable public WorkspaceEditCapability workspaceEdit;

    /** Capabilities specific to the `workspace/didChangeConfiguration` notification. */
    @Nullable public DynamicRegistrationCapability didChangeConfiguration;

    /** Capabilities specific to the `workspace/didChangeWatchedFiles` notification. */
    @Nullable public DynamicRegistrationCapability didChangeWatchedFiles;

    /** Capabilities specific to the `workspace/symbol` request. */
    @Nullable public DynamicRegistrationCapability symbol;

    /** Capabilities specific to the `workspace/executeCommand` request. */
    @Nullable public DynamicRegistrationCapability executeCommand;
  }

  public static class TextDocumentClientCapabilities {
    @Nullable public SynchronizationCapabilities synchronization;

    /** Capabilities specific to the `textDocument/completion` */
    @Nullable public CompletionCapabilities completion;

    /** Capabilities specific to the `textDocument/hover` */
    @Nullable public DynamicRegistrationCapability hover;

    /** Capabilities specific to the `textDocument/signatureHelp` */
    @Nullable public DynamicRegistrationCapability signatureHelp;

    /** Capabilities specific to the `textDocument/references` */
    @Nullable public DynamicRegistrationCapability references;

    /** Capabilities specific to the `textDocument/documentHighlight` */
    @Nullable public DynamicRegistrationCapability documentHighlight;

    /** Capabilities specific to the `textDocument/documentSymbol` */
    @Nullable public DynamicRegistrationCapability documentSymbol;

    /** Capabilities specific to the `textDocument/formatting` */
    @Nullable public DynamicRegistrationCapability formatting;

    /** Capabilities specific to the `textDocument/rangeFormatting` */
    @Nullable public DynamicRegistrationCapability rangeFormatting;

    /** Capabilities specific to the `textDocument/onTypeFormatting` */
    @Nullable public DynamicRegistrationCapability onTypeFormatting;

    /** Capabilities specific to the `textDocument/definition` */
    @Nullable public DynamicRegistrationCapability definition;

    /** Capabilities specific to the `textDocument/codeAction` */
    @Nullable public DynamicRegistrationCapability codeAction;

    /** Capabilities specific to the `textDocument/codeLens` */
    @Nullable public DynamicRegistrationCapability codeLens;

    /** Capabilities specific to the `textDocument/documentLink` */
    @Nullable public DynamicRegistrationCapability documentLink;

    /** Capabilities specific to the `textDocument/rename` */
    @Nullable public DynamicRegistrationCapability rename;
  }

  public static class DynamicRegistrationCapability {
    /** Execute command supports dynamic registration. */
    @Nullable public boolean dynamicRegistration;
  }

  public static class WorkspaceEditCapability {
    /** The client supports versioned document changes in `WorkspaceEdit`s */
    @Nullable public boolean documentChanges;
  }

  public static class SynchronizationCapabilities extends DynamicRegistrationCapability {
    /** The client supports sending will save notifications. */
    @Nullable public boolean willSave;

    /**
     * The client supports sending a will save request and waits for a response providing text edits
     * which will be applied to the document before it is saved.
     */
    @Nullable public boolean willSaveWaitUntil;

    /** The client supports did save notifications. */
    @Nullable public boolean didSave;
  }

  public static class CompletionCapabilities extends DynamicRegistrationCapability {
    /** The client supports the following `CompletionItem` specific capabilities. */
    @Nullable public CompletionItemCapabilities completionItem;
  };

  public static class CompletionItemCapabilities {
    /**
     * Client supports snippets as insert text.
     *
     * <p>A snippet can define tab stops and placeholders with `$1`, `$2` and `${3:foo}`. `$0`
     * defines the final tab stop, it defaults to the end of the snippet. Placeholders with equal
     * identifiers are linked, that is typing in one will update others too.
     */
    @Nullable public boolean snippetSupport;
  }
}
