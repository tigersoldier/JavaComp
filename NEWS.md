## v1.1.1

This is a buf fix release.

* Fixed crash on Java 9 caused by usage of JDK internal packages.
* Fixed parsing .class files with anonymous classes.
* Completion between two dots now works as intended.

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
