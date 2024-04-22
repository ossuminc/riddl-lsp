package com.ossuminc.riddl.lsp.server

import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.DocContentMgmt.{
  ActionsAndLenses,
  CompletionAndHover
}
import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.{
  DocLifecycleMgmt,
  RiddlLSPTextDocumentService,
  Vars
}
import com.ossuminc.riddl.lsp.utils.Utils
import com.ossuminc.riddl.lsp.utils.Utils.createInitializeResultIncremental
import org.eclipse.lsp4j.{CompletionOptions, InitializeParams, InitializeResult}
import org.eclipse.lsp4j.services.{
  LanguageServer,
  TextDocumentService,
  WorkspaceService
}

import java.util.concurrent.CompletableFuture

class RiddlLSPServer extends LanguageServer {
  private val documentService = new RiddlLSPTextDocumentService()
  private val workspaceService = new RiddlLSPWorkspaceService()

  override def initialize(
      params: InitializeParams
  ): CompletableFuture[InitializeResult] = {
    val result = createInitializeResultIncremental()
    CompletableFuture.supplyAsync(() => result)
  }

  override def shutdown(): CompletableFuture[AnyRef] =
    CompletableFuture.supplyAsync(() => null)

  override def exit(): Unit = ()

  override def getTextDocumentService: TextDocumentService = documentService

  override def getWorkspaceService: WorkspaceService = workspaceService
}
