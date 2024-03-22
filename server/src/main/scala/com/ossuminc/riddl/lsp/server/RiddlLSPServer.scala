package com.ossuminc.riddl.lsp.server

import org.eclipse.lsp4j.{CompletionOptions, InitializeParams, InitializeResult, ServerCapabilities, TextDocumentSyncKind}
import org.eclipse.lsp4j.services.{LanguageServer, TextDocumentService, WorkspaceService}

import java.util.concurrent.CompletableFuture

class RiddlLSPServer extends LanguageServer {
  var errorCode = 0

  val documentService = new RiddlLSPTextDocumentService()
  val workspaceService = new RiddlLSPWorkspaceService()

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = {
    val result = new InitializeResult(new ServerCapabilities())

    result.getCapabilities.setTextDocumentSync(TextDocumentSyncKind.Full)
    result.getCapabilities.setCompletionProvider(new CompletionOptions())
    CompletableFuture.supplyAsync(() => result)
  }

  override def shutdown(): CompletableFuture[AnyRef] = {
    errorCode = 0
    null
  }

  override def exit(): Unit = {
    System.exit(errorCode)
  }

  override def getTextDocumentService: TextDocumentService = ???

  override def getWorkspaceService: WorkspaceService = ???
}
