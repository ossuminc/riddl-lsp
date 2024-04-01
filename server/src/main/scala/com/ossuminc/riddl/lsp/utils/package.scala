package com.ossuminc.riddl.lsp

import org.eclipse.lsp4j.{CompletionOptions, InitializeResult, ServerCapabilities, TextDocumentSyncKind}

package object utils {
  def createInitializeResultIncremental(): InitializeResult = {
    val result = new InitializeResult(new ServerCapabilities())

    result.getCapabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental)
    result.getCapabilities.setCompletionProvider(new CompletionOptions())
    result
  }
}
