package com.ossum.riddl.lsp.server

import com.ossum.riddl.lsp
import com.ossum.riddl.lsp.utils.DocumentIdentifierSpec
import org.eclipse.lsp4j.{
  CompletionItem,
  CompletionList,
  CompletionParams,
  DocumentDiagnosticParams,
  DocumentDiagnosticReport,
  Position
}

import java.util
import java.util.concurrent.CompletableFuture
import org.eclipse.lsp4j.jsonrpc.messages

import scala.concurrent.Future
import scala.jdk.FutureConverters.*

object RequestSpecs {
  trait DiagnosticRequestSpec extends DocumentIdentifierSpec {
    val request = new DocumentDiagnosticParams(textDocumentIdentifier)

    val diagnosticResultF: CompletableFuture[DocumentDiagnosticReport] =
      service.diagnostic(request)
  }

  trait CompletionRequestSpec extends DocumentIdentifierSpec {
    var completionResultF: CompletableFuture[
      messages.Either[util.List[CompletionItem], CompletionList]
    ] = Future.failed(new Exception()).asJava.toCompletableFuture

    def requestCompletion(): Unit = {
      params.setPosition(position)

      completionResultF = service.completion(params)
    }

    val params = new CompletionParams()
    params.setTextDocument(textDocumentIdentifier)
    val position = new Position()
  }
}
