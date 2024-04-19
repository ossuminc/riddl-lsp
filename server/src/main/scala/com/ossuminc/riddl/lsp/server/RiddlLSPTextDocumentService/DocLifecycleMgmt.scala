package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.lsp.utils.parseFromURI
import com.ossuminc.riddl.lsp.utils.implicits.*
import com.ossuminc.riddl.lsp.utils.parsing.{
  parseDocFromSource,
  parseDocFromString
}

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
    vars.docLines =
      if vars.riddlDoc.isDefined then
        vars.riddlDoc.getOrElse("").getLinesFromText
      else Seq()
  }

  private def updateParsedDoc(
      fromURL: Boolean = true
  ): Unit = {
    vars.docAST =
      if fromURL then vars.docURI.flatMap(parseDocFromSource)
      else vars.riddlDoc.flatMap(parseDocFromString)

    updateDocLines()
  }

  private def updateRIDDLDocFromURI(
      docURI: String
  ): Unit = {
    val data = parseFromURI(docURI)
    vars.riddlDoc = if data.nonEmpty then Some(data) else None
  }

  def checkMessagesInASTAndFailOrDo[T](
      requestURI: String,
      doOnMessages: (msgs: Messages) => Future[T]
  ): CompletableFuture[T] = {
    val astOpt: Option[Either[Messages, AST.Root]] =
      if !vars.docURI.contains(requestURI) then {
        parseDocFromSource(requestURI)
      } else vars.docAST

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

  def didOpen(
      params: DidOpenTextDocumentParams
  ): Unit = {
    vars.riddlDoc = Some(params.getTextDocument.getText)
    vars.docURI = Some(params.getTextDocument.getUri)
    updateParsedDoc()
  }

  def didChange(
      params: DidChangeTextDocumentParams
  ): Unit = {
    def patchLine(
        line: String,
        text: String,
        range: lsp4j.Range = lsp4j.Range()
    ): String =
      line.patch(
        range.getStart.getCharacter - 1,
        text,
        range.getEnd.getCharacter - range.getStart.getCharacter
      )

    vars.riddlDoc = vars.riddlDoc.map { doc =>
      val changes = params.getContentChanges.asScala.toSeq
      var docLines: Seq[String] = doc.linesIterator.toSeq

      if docLines.nonEmpty then {
        changes.foreach { change =>
          val changeRangeStart: Position =
            change.getRange.getStart
          val changeRangeEnd: Position = change.getRange.getEnd
          val changesToPatch: Seq[String] = docLines.slice(
            changeRangeStart.getLine - 1,
            changeRangeEnd.getLine
          )
          val changeLines = change.getText.getLinesFromText

          val startLinePatch: String =
            patchLine(
              changesToPatch.head,
              changeLines.head,
              change.getRange
            )
          val endPatch: Seq[String] =
            changesToPatch.zip(changeLines).drop(1).map { (toPatch, patch) =>
              patchLine(toPatch, patch)
            }

          val finalPatch = startLinePatch +: endPatch
          docLines = docLines
            .patch(
              changeRangeStart.getLine - 1, // start of replacement
              finalPatch,
              changeRangeEnd.getLine - changeRangeStart.getLine + 1 // length to replace (will be deleted)
            )
        }
      } else docLines = changes.map(_.getText)
      docLines.mkString("\n")
    }
    updateParsedDoc(false)
  }

  def didClose(
      params: DidCloseTextDocumentParams
  ): Unit = {
    vars.riddlDoc = None
    updateParsedDoc(false)
  }

  def didSave(
      params: DidSaveTextDocumentParams
  ): Unit = {
    updateRIDDLDocFromURI(params.getTextDocument.getUri)
    updateParsedDoc()
  }
}
