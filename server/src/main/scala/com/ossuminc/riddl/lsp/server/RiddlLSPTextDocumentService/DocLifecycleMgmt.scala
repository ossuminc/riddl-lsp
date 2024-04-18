package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossum.riddl.lsp.server.InitializationSpecs.*
import org.eclipse.lsp4j
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.jsonrpc.messages
import org.scalatest.concurrent.Futures.{whenReady, whenReadyImpl}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.language.postfixOps

object DocLifecycleMgmt {

  // Updating Vars
  private def updateDocLines(): Unit = {
    docLines =
      if riddlDoc.isDefined then riddlDoc.getOrElse("").getLinesFromText
      else Seq()
  }

  private def updateParsedDoc(fromURL: Boolean = true): Unit = {
    if fromURL then docAST = docURI.flatMap(parseDocFromSource)
    else docAST = riddlDoc.flatMap(parseDocFromString)

    updateDocLines()
  }

  private def updateRIDDLDocFromURI(
      uri: String = docURI.getOrElse("")
  ): Unit = {
    val data = parseFromURI(uri)
    riddlDoc = if data.nonEmpty then Some(data) else None
  }

  private def checkMessagesInASTAndFailOrDo[T](
      requestURI: String,
      doOnMessages: (msgs: Messages) => Future[T]
  ): CompletableFuture[T] = {
    val astOpt: Option[Either[Messages, AST.Root]] =
      if !docURI.contains(requestURI) then {
        parseDocFromSource(requestURI)
      } else docAST

    val resultF: Future[T] = astOpt match {
      case Some(ast) =>
        if ast.isRight then
          Future.failed(new Throwable("Document has no errors"))
        else {
          ast match {
            case Left(msgs) => doOnMessages(msgs)
            case _ => Future.failed(Throwable("No errors found in document"))
          }
        }
      case None =>
        Future.failed(
          if parseDocFromSource(
              requestURI
            ).isEmpty
          then new Throwable("Document is empty")
          else new Throwable("Document is closed")
        )
    }

    resultF.asJava.toCompletableFuture
  }
  def didOpen(params: DidOpenTextDocumentParams): Unit = {
    riddlDoc = Some(params.getTextDocument.getText)
    docURI = Some(params.getTextDocument.getUri)
    updateParsedDoc()
  }

  def didChange(params: DidChangeTextDocumentParams): Unit = {
    def patchLine(line: String, range: lsp4j.Range, text: String) = line.patch(
      range.getStart.getCharacter,
      text,
      range.getEnd.getCharacter - range.getStart.getCharacter
    )

    riddlDoc = riddlDoc.map(doc =>
      var docLines: Seq[String] = doc.linesIterator.toSeq
      val changes = params.getContentChanges.asScala.toSeq
      if docLines.nonEmpty then
        changes.foreach { change =>
          val changeRangeStart: Position = change.getRange.getStart
          val changeRangeEnd: Position = change.getRange.getEnd
          val changesToPatch: Seq[String] = docLines.slice(
            changeRangeStart.getLine,
            changeRangeEnd.getLine
          )
          val changeLines = change.getText.getLinesFromText
          val startLinePatch: String =
            patchLine(docLines.head, change.getRange, changeLines.head)
          val middlePatch: Seq[String] = changeLines.slice(1, -1)
          val endLinePatch: String =
            patchLine(docLines.last, change.getRange, changeLines.last)

          val finalPatch = startLinePatch +: middlePatch :+ endLinePatch
          docLines = docLines.patch(
            changeRangeStart.getLine, // start of replacement
            startLinePatch +: middlePatch :+ endLinePatch,
            changeRangeEnd.getLine - changeRangeStart.getLine // length to replace (will be deleted)
          )
        }
      else docLines = Seq(changes.map(_.getText).mkString("\n"))
      docLines.mkString
    )
    updateParsedDoc()
  }

  def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = None
    updateParsedDoc(false)
  }

  def didSave(params: DidSaveTextDocumentParams): Unit = {
    updateRIDDLDocFromURI(params.getTextDocument.getUri)
    updateParsedDoc()
  }
}
