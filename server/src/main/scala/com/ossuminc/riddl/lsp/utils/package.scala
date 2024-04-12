package com.ossuminc.riddl.lsp

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

package object utils {
  def parseFromURI(uri: String): String = {
    val source = io.Source.fromURL(uri)
    try {
      source.getLines().mkString("\n")
    } finally {
      source.close()
    }
  }

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
}
