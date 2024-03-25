package com.ossuminc.riddl.lsp.server

import com.ossuminc.riddl.lsp.utils.createInitializeResultIncremental
import org.eclipse.lsp4j.{CompletionOptions, InitializeParams, InitializeResult, ServerCapabilities, TextDocumentSyncKind}
import org.eclipse.lsp4j.services.{LanguageServer, TextDocumentService, WorkspaceService}

import java.util.concurrent.CompletableFuture

class RiddlLSPServer extends LanguageServer {
  val documentService = new RiddlLSPTextDocumentService()
  val workspaceService = new RiddlLSPWorkspaceService()

  override def initialize(params: InitializeParams): CompletableFuture[InitializeResult] = {
    val result = createInitializeResultIncremental()
    CompletableFuture.supplyAsync(() => result)
  }

  override def shutdown(): CompletableFuture[AnyRef] = {
    CompletableFuture.supplyAsync(() => null)
  }

  override def exit(): Unit = {
    ()
  }

  override def getTextDocumentService: TextDocumentService = new RiddlLSPTextDocumentService()

  override def getWorkspaceService: WorkspaceService = new RiddlLSPWorkspaceService()
}
