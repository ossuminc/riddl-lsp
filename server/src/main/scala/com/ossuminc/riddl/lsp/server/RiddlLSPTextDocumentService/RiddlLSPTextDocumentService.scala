package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.DocContentMgmt.{
  ActionsAndLenses,
  CompletionAndHover
}
import com.ossuminc.riddl.lsp.utils.implicits.*
import com.ossuminc.riddl.lsp.utils.parseFromURI
import org.eclipse.lsp4j
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.services.TextDocumentService

import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.xml.include.UnavailableResourceException

class RiddlLSPTextDocumentService extends TextDocumentService {
  private var docURI: Option[String] = None
  private var riddlDoc: Option[String] = None
  private var docLines: Seq[String] = Seq()
  private var docAST: Option[Either[Messages, AST.Root]] = None

  override def completion(position: CompletionParams): CompletableFuture[
    messages.Either[util.List[CompletionItem], CompletionList]
  ] = CompletionAndHover.completion(position)

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
