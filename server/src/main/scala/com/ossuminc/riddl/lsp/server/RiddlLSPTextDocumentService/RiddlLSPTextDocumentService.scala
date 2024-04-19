package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.DocContentMgmt.{
  ActionsAndLenses,
  CompletionAndHover
}
import org.eclipse.lsp4j
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.services.TextDocumentService

import java.util
import java.util.concurrent.CompletableFuture

class RiddlLSPTextDocumentService extends TextDocumentService {
  override def completion(position: CompletionParams): CompletableFuture[
    messages.Either[util.List[CompletionItem], CompletionList]
  ] = CompletionAndHover.completion(
    position
  )

  override def didOpen(params: DidOpenTextDocumentParams): Unit =
    DocLifecycleMgmt.didOpen(params)

  override def didChange(params: DidChangeTextDocumentParams): Unit =
    DocLifecycleMgmt.didChange(params)

  override def didClose(params: DidCloseTextDocumentParams): Unit =
    DocLifecycleMgmt.didClose(params)

  override def didSave(params: DidSaveTextDocumentParams): Unit =
    DocLifecycleMgmt.didSave(params)

  // TODO: uncomment and finish when appropriate
  /*
  override def references(
      params: ReferenceParams
  ): CompletableFuture[util.List[_ <: Location]] = {
    var foundLocations: Seq[Location] = Seq()
    riddlDoc.foreach(doc => {
      val docLines: Seq[String] = doc.linesIterator.toSeq

    })
    if riddlDoc.isDefined then completableFutureWithSeq(foundLocations)
    else completableFutureWithSeq(Seq[Location]())
  }
   */

  override def diagnostic(
      params: DocumentDiagnosticParams
  ): CompletableFuture[DocumentDiagnosticReport] =
    ActionsAndLenses.diagnostic(params)
}
