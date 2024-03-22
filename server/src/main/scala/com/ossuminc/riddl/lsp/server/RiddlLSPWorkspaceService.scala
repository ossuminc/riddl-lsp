package com.ossuminc.riddl.lsp.server

import org.eclipse.lsp4j.{DidChangeConfigurationParams, DidChangeWatchedFilesParams}
import org.eclipse.lsp4j.services.WorkspaceService

object RiddlLSPWorkspaceService extends WorkspaceService {
  override def didChangeConfiguration(params: DidChangeConfigurationParams): Unit = ???

  override def didChangeWatchedFiles(params: DidChangeWatchedFilesParams): Unit = ???
}
