package com.ossuminc.riddl.lsp.server

import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import com.ossuminc.riddl.lsp.utils.implicits.*
import com.ossuminc.riddl.lsp.utils.parseFromURI
import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.*
import org.eclipse.lsp4j.services.TextDocumentService

import scala.concurrent.ExecutionContext.Implicits.global
import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.xml.include.UnavailableResourceException

def getRootFromUri(uri: String) = {
  uri.split("/riddl/").drop(-1).mkString + "riddl/"
}

def parseDocFromSource(docURI: String): Option[Either[Messages, AST.Root]] = {
  val riddlRootDoc = java.net.URI.create(docURI).toURL

  // need to parse doc before giving to riddl parser to check if whole doc is empty (leads to parser failure)
  if parseFromURI(docURI).nonEmpty then
    Some(new TopLevelParser(RiddlParserInput(riddlRootDoc)).parseRoot())
  else None
}

def parseDocFromString(docString: String): Option[Either[Messages, AST.Root]] =
  if docString.nonEmpty then
    Some(new TopLevelParser(RiddlParserInput(docString)).parseRoot())
  else None

class RiddlLSPTextDocumentService extends TextDocumentService {
  private var docURI: Option[String] = None
  private var riddlDoc: Option[String] = None
  private var docLines: Seq[String] = Seq()
  private var docAST: Option[Either[Messages, AST.Root]] = None

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

  // Functionality
  override def completion(position: CompletionParams): CompletableFuture[
    messages.Either[util.List[CompletionItem], CompletionList]
  ] = checkMessagesInASTAndFailOrDo[
    messages.Either[util.List[CompletionItem], CompletionList]
  ](
    position.getTextDocument.getUri,
    msgs => {
      val completionsOpt = getCompletionFromAST(
        msgs,
        position.getPosition.getLine,
        position.getPosition.getCharacter
      )
      if completionsOpt.isDefined then
        Future
          .successful(
            completionsOpt.getOrElse(messages.Either.forLeft(Seq().asJava))
          )
      else
        Future.failed(
          new Throwable(
            "Requested position in document does not have an error"
          )
        )
    }
  )

  private def getCompletionFromAST(
      msgs: List[Messages.Message],
      line: Int,
      charPosition: Int
  ): Option[messages.Either[util.List[CompletionItem], CompletionList]] = {
    var completions = CompletionList()
    val completionList = new CompletionList()
    val grouped = msgs.groupBy(msg => msg.loc.line)
    val lineItems =
      if grouped.isDefinedAt(line) then Some(grouped(line)) else None
    val selectedItem: Seq[Messages.Message] =
      lineItems
        .map { items =>
          val filtered = items.filter(_.loc.col == charPosition)
          if filtered.nonEmpty then filtered else Seq()
        }
        .getOrElse(Seq())

    if selectedItem.nonEmpty then {
      completionList.setItems(
        selectedItem
          .map(msgToCompletion)
          .asJava
      )
      Some(
        messages.Either.forRight(
          completionList
        )
      )
    } else None

  }

  private def msgToCompletion(msg: Messages.Message): CompletionItem = {
    val item = new CompletionItem()
    item.setDetail(msg.message)
    item
  }

  override def didOpen(params: DidOpenTextDocumentParams): Unit = {
    riddlDoc = Some(params.getTextDocument.getText)
    docURI = Some(params.getTextDocument.getUri)
    updateParsedDoc()
  }

  override def didChange(params: DidChangeTextDocumentParams): Unit = {
    def patchLine(line: String, range: lsp4j.Range, text: String) = line.patch(
      range.getStart.getCharacter - 1,
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
            changeRangeStart.getLine - 1,
            changeRangeEnd.getLine
          )
          val changeLines = change.getText.getLinesFromText
          val startLinePatch: String =
            patchLine(changesToPatch.head, change.getRange, changeLines.head)
          val middlePatch: Seq[String] =
            if changeRangeEnd.getLine - changeRangeStart.getLine < 1 then
              changesToPatch.slice(
                1,
                changeRangeEnd.getLine
              )
            else Seq()

          val finalPatch = startLinePatch +: middlePatch
          docLines = docLines.patch(
            changeRangeStart.getLine - 1, // start of replacement
            finalPatch,
            changeRangeEnd.getLine - changeRangeStart.getLine + 1 // length to replace (will be deleted)
          )
        }
      else docLines = changes.map(_.getText)
      docLines.mkString("\n")
    )
    updateParsedDoc(false)
  }

  override def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = None
    updateParsedDoc(false)
  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    updateRIDDLDocFromURI(params.getTextDocument.getUri)
    updateParsedDoc()
  }

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
  ): CompletableFuture[DocumentDiagnosticReport] = {
    def msgToSeverity(msg: Messages.Message): DiagnosticSeverity =
      msg.kind match {
        case Messages.Info           => DiagnosticSeverity.Information
        case Messages.Error          => DiagnosticSeverity.Error
        case Messages.SevereError    => DiagnosticSeverity.Error
        case Messages.UsageWarning   => DiagnosticSeverity.Warning
        case Messages.StyleWarning   => DiagnosticSeverity.Warning
        case Messages.MissingWarning => DiagnosticSeverity.Warning
        case Messages.Warning        => DiagnosticSeverity.Warning
      }

    docAST
      .map { ast =>
        checkMessagesInASTAndFailOrDo[DocumentDiagnosticReport](
          params.getTextDocument.getUri,
          (msgs: Messages) => {
            val diagnosticList: List[Diagnostic] = msgs.map { msg =>
              val diagnostic: Diagnostic = new Diagnostic()
              diagnostic.setSeverity(msgToSeverity(msg))
              diagnostic.setMessage(msg.message)
              diagnostic.setRange(msg.toLSP4JRange)
              diagnostic
            }
            Future
              .successful(
                new DocumentDiagnosticReport(
                  new RelatedFullDocumentDiagnosticReport(diagnosticList.asJava)
                )
              )
          }
        )
      }
      .getOrElse(
        Future
          .failed(new UnavailableResourceException())
          .asJava
          .toCompletableFuture
      )
  }
}
