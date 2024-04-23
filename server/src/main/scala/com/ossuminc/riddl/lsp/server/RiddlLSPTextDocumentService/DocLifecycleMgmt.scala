package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossuminc.riddl.lsp.utils.implicits.*
import com.ossuminc.riddl.lsp.utils.parsing
import com.ossuminc.riddl.lsp.utils.parsing.{
  parseDocFromSource,
  parseDocFromString,
  parseFromURI
}
import org.eclipse.lsp4j
import org.eclipse.lsp4j.*

import java.util
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.language.postfixOps

object DocLifecycleMgmt {
  import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.Vars.*

  // Updating Vars
  private def updateDocLines(): Unit = {
    docLines =
      if riddlDoc.isDefined then riddlDoc.getOrElse("").getLinesFromText
      else Seq()

  }

  private def updateParsedDoc(
      fromURL: Boolean = true
  ): Unit = {
    docAST =
      if fromURL then docURI.flatMap(parseDocFromSource)
      else riddlDoc.flatMap(parseDocFromString)

    updateDocLines()
  }

  private def updateRIDDLDocFromURI(
      docURI: String
  ): Unit = {
    val data = parseFromURI(docURI)
    riddlDoc = if data.nonEmpty then Some(data) else None
  }

  def didOpen(
      params: DidOpenTextDocumentParams
  ): Unit = {
    riddlDoc = Some(params.getTextDocument.getText)
    docURI = Some(params.getTextDocument.getUri)
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

    riddlDoc = riddlDoc.map { doc =>
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
    riddlDoc = None
    updateParsedDoc(false)
  }

  def didSave(
      params: DidSaveTextDocumentParams
  ): Unit = {
    updateRIDDLDocFromURI(params.getTextDocument.getUri)
    updateParsedDoc()
  }
}
