## v1.1.0

* JavaComp can parse .class files in .jar files now.
* Auto-import: Completing not imported classes will add import statements.
* Support `completionItem/resolve` command. Currently this command only resolves
  `additionalTextEdit` for auto-imports.
* Support `InsertTextFormat.Snippet`. When a client reports snippet support in
  the completion capabilities, JavaComp returns `insertText` in snippet format.
  When completing methods, snippets are returned for tab stops for each
  arguments.

## v1.0.0

Initial release.
