package com.ossuminc.riddl.lsp.server

import com.ossuminc.riddl.language.{AST, Messages}
import com.ossuminc.riddl.language.Messages.{Messages, logMessages}
import com.ossuminc.riddl.language.parsing.{
  RiddlParserInput,
  StringParserInput,
  TopLevelParser
}
import com.ossuminc.riddl.lsp.utils.implicits.*
import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{
  CompletionItem,
  CompletionList,
  CompletionParams,
  Diagnostic,
  DiagnosticRelatedInformation,
  DiagnosticSeverity,
  DidChangeTextDocumentParams,
  DidCloseTextDocumentParams,
  DidOpenTextDocumentParams,
  DidSaveTextDocumentParams,
  DocumentDiagnosticParams,
  DocumentDiagnosticReport,
  FullDocumentDiagnosticReport,
  Position,
  RelatedFullDocumentDiagnosticReport
}
import org.eclipse.lsp4j.services.TextDocumentService

import scala.concurrent.ExecutionContext.Implicits.global
import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.io.Source
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.xml.include.UnavailableResourceException

def getRootFromUri(uri: String) = {
  uri.split("/riddl/").drop(-1).mkString + "riddl/"
}

def parseDocFromSource(docURI: String): Either[Messages, AST.Root] = {
  val riddlRootDoc = java.net.URI.create(docURI).toURL
  new TopLevelParser(RiddlParserInput(riddlRootDoc)).parseRoot()
}

def parseDocFromString(docString: String): Either[Messages, AST.Root] =
  new TopLevelParser(StringParserInput(docString)).parseRoot()

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
    if fromURL then docAST = docURI.map(parseDocFromSource)
    else docAST = riddlDoc.map(parseDocFromString)

    updateDocLines()
  }

  private def updateRIDDLDocFromURI(): Unit = docURI.foreach { uri =>
    val source = io.Source.fromURL(uri)
    lazy val data: String =
      try {
        source.mkString
      } finally {
        source.close()
      }
    riddlDoc = if data.nonEmpty then Some(data) else None
  }

  private def checkMessagesInASTAndFailOrDo[T](
      uri: String,
      doOnMessages: (msgs: Messages) => Future[T]
  ): CompletableFuture[T] = {
    val ast: Either[Messages, AST.Root] =
      if !docURI.contains(uri) then parseDocFromSource(uri)
      else docAST.getOrElse(Right(AST.Root()))

    val resultF: Future[T] =
      if ast.isRight then Future.failed(UnavailableResourceException())
      else {
        ast match {
          case Left(msgs) => doOnMessages(msgs)
          case _          => Future.failed(Throwable())
        }
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
      else Future.failed(Throwable())
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
          val filtered = items.filterNot(_.loc.offset == charPosition)
          if filtered.nonEmpty then filtered else Seq()
        }
        .getOrElse(Seq())

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

  override def didClose(params: DidCloseTextDocumentParams): Unit = {
    riddlDoc = None
    updateParsedDoc(false)
  }

  override def didSave(params: DidSaveTextDocumentParams): Unit = {
    updateRIDDLDocFromURI()
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
              val relatedInfo = new DiagnosticRelatedInformation()
              relatedInfo.setMessage(msg.context)
              val location = msg.toLSP4JLocation
              location.setUri(params.getTextDocument.getUri)
              relatedInfo.setLocation(location)
              val diagnostic = new Diagnostic()
              diagnostic.setSeverity(msgToSeverity(msg))
              diagnostic.setMessage(msg.message)
              diagnostic.setRelatedInformation(Seq(relatedInfo).asJava)
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
