package com.ossuminc.riddl.lsp.server

import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.parsing.{SourceParserInput, StringParserInput, TopLevelParser}
import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{CompletionItem, CompletionList, CompletionParams, DidChangeTextDocumentParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, Position, TextDocumentContentChangeEvent}
import org.eclipse.lsp4j.services.TextDocumentService

import scala.concurrent.ExecutionContext.Implicits.global
import java.util
import java.util.concurrent.CompletableFuture
import scala.jdk.CollectionConverters.*

def parseDocFromSource(docURI: String): Either[Messages, AST.Root] = {
  val riddlRootURI = docURI.split("/riddl/").drop(-1).mkString + "riddl/"
  val riddlRootDoc = io.Source.fromURL(riddlRootURI)
  new TopLevelParser(SourceParserInput(riddlRootDoc, docURI)).parseRoot()
}

def parseDocFromString(docString: String): Either[Messages, AST.Root] =
  new TopLevelParser(StringParserInput(docString)).parseRoot()

class RiddlLSPTextDocumentService extends TextDocumentService {
  private var docURI: Option[String] = None
  private var riddlDoc: Option[String] = None
  private var parsedDoc: Option[Either[Messages, AST.Root]] = None

  def updateParsedDoc(fromURI: Boolean = true): Unit = {
    if fromURI then parsedDoc = docURI.map(parseDocFromSource)
    else parsedDoc = riddlDoc.map(parseDocFromString)
  }

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
    riddlDoc = Some(params.getTextDocument.getText)
    docURI = Some(params.getTextDocument.getUri)
    updateParsedDoc()
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    def patchLine(line: String, range: lsp4j.Range, text: String) = line.patch(
      range.getStart.getCharacter,
      text,
      range.getEnd.getCharacter - range.getStart.getCharacter
    )

    val changes: Seq[TextDocumentContentChangeEvent] = params.getContentChanges.asScala.toSeq

    riddlDoc = riddlDoc.map(doc =>
      var docLines: Seq[String] = doc.linesIterator.toSeq
      changes.foreach(change => {
        val changeRangeStart: Position = change.getRange.getStart
        val changeRangeEnd: Position = change.getRange.getEnd
        val changesToPatch: Seq[String] = docLines.slice(
          changeRangeStart.getLine,
          changeRangeEnd.getLine
        )
        val changeLines = change.getText.linesIterator.toSeq
        val startLinePatch: String = patchLine(docLines.head, changes.head.getRange, changeLines.head)
        val middlePatch: Seq[String] = changeLines.slice(1, -1)
        val endLinePatch: String = patchLine(docLines.last, changes.last.getRange, changeLines.last)
        docLines = docLines.patch(
          changeRangeStart.getLine,
          startLinePatch +: middlePatch :+ endLinePatch,
          changeRangeEnd.getLine - changeRangeStart.getLine
        )
      })
      docLines.mkString
    )
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = None
    updateParsedDoc()
  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    parsedDoc = parsedDoc.map(_ => parseDocFromSource(params.getTextDocument.getUri))
  }
}