package com.ossuminc.riddl.lsp.server

import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{CompletionItem, CompletionList, CompletionParams, DidChangeTextDocumentParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams}
import org.eclipse.lsp4j.services.TextDocumentService

import java.util
import java.util.concurrent.CompletableFuture

object RiddlLSPTextDocumentService extends TextDocumentService {

  override def completion(position: CompletionParams):
    CompletableFuture[messages.Either[util.List[CompletionItem], CompletionList]] = {
    CompletableFuture.supplyAsync(() => {
     List[CompletionItem].empty()
    })
  }
  override def didOpen(params: DidOpenTextDocumentParams): Unit = ???

  override def didChange(params: DidChangeTextDocumentParams): Unit = ???

  override def didClose(params: DidCloseTextDocumentParams): Unit = ???

  override def didSave(params: DidSaveTextDocumentParams): Unit = ???
}