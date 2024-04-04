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
import scala.io.Source
import scala.jdk.CollectionConverters.*

def getRootFromUri(uri: String) = {
  uri.split("/riddl/").drop(-1).mkString + "riddl/"
}

def parseDocFromSource(docURI: String): Either[Messages, AST.Root] = {
  val riddlRootDoc = io.Source.fromURL(docURI)
  new TopLevelParser(SourceParserInput(riddlRootDoc, docURI)).parseRoot()
}

def parseDocFromString(docString: String): Either[Messages, AST.Root] =
  new TopLevelParser(StringParserInput(docString)).parseRoot()

class RiddlLSPTextDocumentService extends TextDocumentService {
  private var docURI: Option[String] = None
  private var riddlDoc: Option[String] = None
  private var parsedDoc: Option[Either[Messages, AST.Root]] = None

  private def updateParsedDoc(fromURI: Boolean = true): Unit = {
    if fromURI then parsedDoc = riddlDoc.map(parseDocFromString)
    else parsedDoc = docURI.map(parseDocFromSource)
  }

  private def updateRIDDLDocFromURI(): Unit = docURI.foreach(uri => {
    val source = io.Source.fromURL(uri)
    lazy val data: String =
      try {
        source.mkString
      }
      finally {
        source.close()
      }
    riddlDoc = if data.nonEmpty then Some(data) else None
  })

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
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    def patchLine(line: String, range: lsp4j.Range, text: String) = line.patch(
      range.getStart.getCharacter,
      text,
      range.getEnd.getCharacter - range.getStart.getCharacter
    )

    riddlDoc = riddlDoc.map(doc =>
      var docLines: Seq[String] = doc.linesIterator.toSeq
      val changes = params.getContentChanges.asScala.toSeq
      if docLines.nonEmpty then changes.foreach(change => {
        val changeRangeStart: Position = change.getRange.getStart
        val changeRangeEnd: Position = change.getRange.getEnd
        val changesToPatch: Seq[String] = docLines.slice(
          changeRangeStart.getLine,
          changeRangeEnd.getLine
        )
        val changeLines = change.getText.linesIterator.toSeq
        val startLinePatch: String = patchLine(docLines.head, change.getRange, changeLines.head)
        val middlePatch: Seq[String] = changeLines.slice(1, -1)
        val endLinePatch: String = patchLine(docLines.last, change.getRange, changeLines.last)

        val finalPatch = startLinePatch +: middlePatch :+ endLinePatch
        docLines = docLines.patch(
          changeRangeStart.getLine, //start of replacement
          startLinePatch +: middlePatch :+ endLinePatch,
          changeRangeEnd.getLine - changeRangeStart.getLine //length to replace (will be deleted)
        )
      })
      else docLines = Seq(changes.map(_.getText).mkString("\n"))
      docLines.mkString
    )
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = None
  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    updateRIDDLDocFromURI()
    updateParsedDoc()
  }
}