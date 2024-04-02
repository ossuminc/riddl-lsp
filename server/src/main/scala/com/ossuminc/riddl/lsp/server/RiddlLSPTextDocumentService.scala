package com.ossuminc.riddl.lsp.server

import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{CompletionItem, CompletionList, CompletionParams, DidChangeTextDocumentParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, TextDocumentContentChangeEvent}
import org.eclipse.lsp4j.services.TextDocumentService

import java.util
import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*

case class RiddlCodeDoc(textString: String = "")

object RiddlCodeDoc {
}

class RiddlLSPTextDocumentService extends TextDocumentService {
  private var riddlDoc: RiddlCodeDoc = RiddlCodeDoc()
  private val unsavedChanges: Seq[TextDocumentContentChangeEvent] = Seq()

  override def completion(position: CompletionParams):
    CompletableFuture[messages.Either[util.List[CompletionItem], CompletionList]] = {
      CompletableFuture.supplyAsync(() => {
        org.eclipse.lsp4j.jsonrpc.messages.Either.forLeft(
          java.util.List.of()
        )
      }
    )
  }
  override def didOpen(params: DidOpenTextDocumentParams): Unit = {
    riddlDoc = RiddlCodeDoc(params.getTextDocument.getText)
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    unsavedChanges ++ params.getContentChanges.asScala
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = RiddlCodeDoc()
  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = ???
}