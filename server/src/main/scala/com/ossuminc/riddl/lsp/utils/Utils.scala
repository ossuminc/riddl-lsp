package com.ossuminc.riddl.lsp.utils

import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.lsp.utils.parsing.parseDocFromSource
import org.eclipse.lsp4j.{
  CompletionOptions,
  InitializeResult,
  ServerCapabilities,
  TextDocumentSyncKind
}

import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.language.implicitConversions

object Utils {
  import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.Vars

  def createInitializeResultIncremental(): InitializeResult = {
    val result = new InitializeResult(new ServerCapabilities())

    result.getCapabilities.setTextDocumentSync(TextDocumentSyncKind.Incremental)
    result.getCapabilities.setCompletionProvider(new CompletionOptions())
    result
  }

  def completableFutureWithResult[R](result: R): CompletableFuture[R] =
    Future.successful(result).asJava.toCompletableFuture

  def completableFutureWithSeq[S](
      result: Seq[S]
  ): CompletableFuture[java.util.List[_ <: S]] =
    completableFutureWithResult(result.asJava)

  def checkMessagesInASTAndFailOrDo[T](
      requestURI: String,
      doOnMessages: (msgs: Messages) => Future[T]
  ): CompletableFuture[T] = {
    val astOpt: Option[Either[Messages, AST.Root]] =
      if !Vars.docURI.contains(requestURI) then parseDocFromSource(requestURI)
      else Vars.docAST

    val resultF: Future[T] = astOpt match {
      case Some(ast) =>
        if ast.isRight then
          Future.failed(new Throwable("Document has no errors"))
        else
          ast match {
            case Left(msgs) => doOnMessages(msgs)
            case _ => Future.failed(Throwable("No errors found in document"))
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
}
