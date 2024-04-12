package com.ossum.riddl.lsp.server

import com.ossum.riddl.lsp.server.InitializationSpecs.*
import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{
  CompletionItem,
  CompletionList,
  CompletionParams,
  DidCloseTextDocumentParams,
  DocumentDiagnosticParams,
  DocumentDiagnosticReport,
  DocumentDiagnosticReportKind,
  Position
}
import org.scalatest.concurrent.Futures.whenReady
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future
import scala.language.postfixOps
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

class RiddlLSPTextDocumentSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures {

  trait DiagnosticRequestSpec extends DocumentIdentifierSpec {
    val request = new DocumentDiagnosticParams(textDocumentIdentifier)

    val diagnosticResultF: CompletableFuture[DocumentDiagnosticReport] =
      service.diagnostic(request)
  }

  trait CompletionRequestSpec extends DocumentIdentifierSpec {

    var completionResultF: CompletableFuture[
      messages.Either[util.List[CompletionItem], CompletionList]
    ] = Future.failed(new Throwable()).asJava.toCompletableFuture
    def makeRequest(): Unit = {
      params.setPosition(position)

      completionResultF = service.completion(params)
    }

    val params = new CompletionParams()
    params.setTextDocument(textDocumentIdentifier)
    val position = new Position()

  }

  "RiddlLSPTextDocumentService" must {
    "successfully open everything.riddl, failing to retrieve a completion" in new OpenNoErrorFileSpec
      with CompletionRequestSpec
      with DiagnosticRequestSpec {
      position.setLine(1)
      position.setCharacter(1)
      makeRequest()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
    }

    "successfully open everythingOneError.riddl, retrieving a completion" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {

      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      makeRequest()

      whenReady(completionResultF.asScala) { completion =>
        completion.isRight mustBe true
        completion.getRight.getItems.asScala.length mustEqual 1
        completion.getRight.getItems.asScala.head.getDetail mustEqual "Expected one of (end-of-input | whitespace after keyword)"
      }
    }

    "successfully open empty.riddl, failing to retrieve a completion" in new OpenEmptyFileSpec
      with CompletionRequestSpec {

      position.setLine(1)
      position.setCharacter(1)
      makeRequest()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is empty"
    }

    "successfully close everythingOneError.riddl, failing to retrieve a completion" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {
      val closeNotification: DidCloseTextDocumentParams =
        new DidCloseTextDocumentParams()
      closeNotification.setTextDocument(textDocumentIdentifier)
      service.didClose(closeNotification)

      position.setLine(1)
      position.setCharacter(1)
      makeRequest()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is closed"
    }

    "fail for completion error from a file with no errors" in new OpenNoErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(1)
      position.setCharacter(1)
      makeRequest()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
    }

    "fail for completion from a file with an error" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      makeRequest()

      whenReady(completionResultF.asScala) { eitherList =>
        eitherList.isRight mustBe true
        eitherList.getRight.getItems.asScala.length mustEqual 1
        eitherList.getRight.getItems.asScala.head.getDetail mustEqual
          """Expected one of (end-of-input | whitespace after keyword)"""
      }
    }

    "fail requesting for diagnostic from file with no errors" in new OpenNoErrorFileSpec
      with DiagnosticRequestSpec {

      diagnosticResultF.asScala.failed.futureValue mustBe a[Throwable]
      diagnosticResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
    }

    "succeed requesting for diagnostic from file with one error" in new OpenOneErrorFileSpec
      with DiagnosticRequestSpec {

      whenReady(diagnosticResultF.asScala) { report =>
        report.getRelatedFullDocumentDiagnosticReport.getKind mustEqual DocumentDiagnosticReportKind.Full
        report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.length mustEqual 1
        report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getMessage mustEqual "Expected one of (end-of-input | whitespace after keyword)"
        report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getRange.getStart.getLine mustEqual errorLine
        report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getRange.getStart.getCharacter mustEqual errorCharOnLine
      }
    }

    /* TODO: Finish these tests immediately
    "successfully change everythingOneError.riddl" in new ChangeOneErrorFileSpec {}

    "successfully save everythingOneError.riddl" in new ChangeOneErrorFileSpec {
      val saveNotification = new DidSaveTextDocumentParams()
      saveNotification.setTextDocument(textDocumentIdentifier)
      service.didSave(saveNotification)
    }

    "successfully change empty.riddl" in new ChangeEmptyFileSpec {}

    "successfully save empty.riddl" in new ChangeEmptyFileSpec {
      val saveNotification = new DidSaveTextDocumentParams()
      saveNotification.setTextDocument(textDocumentIdentifier)
      service.didSave(saveNotification)
    }
     */
  }

}
