package com.ossum.riddl.lsp.server.LSPTextDocumentSpec

import com.ossum.riddl.lsp.server.initializationSpecs.*
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
import scala.concurrent.ExecutionContext.Implicits.global

class DocLifecycleMgmtSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures {

  "RiddlLSPTextDocumentService" must {

    "successfully open everythingOneError.riddl, retrieving a completion" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {

      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      requestCompletion()

      whenReady(completionResultF.asScala) { completion =>
        completion.isRight mustBe true
        completion.getRight.getItems.asScala.length mustEqual 1
        completion.getRight.getItems.asScala.head.getDetail mustEqual "Expected one of (end-of-input | whitespace after keyword)"
      }
    }

    "successfully close everythingOneError.riddl, failing to retrieve a completion" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {
      val closeNotification: DidCloseTextDocumentParams =
        new DidCloseTextDocumentParams()
      closeNotification.setTextDocument(textDocumentIdentifier)
      service.didClose(closeNotification)

      position.setLine(1)
      position.setCharacter(1)
      requestCompletion()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is closed"
    }

    "fail for completion error from a file with no errors" in new OpenNoErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(1)
      position.setCharacter(1)
      requestCompletion()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
    }

    "fail for completion from a file with an error" in new OpenOneErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      requestCompletion()

      whenReady(completionResultF.asScala) { completionList =>
        completionList.isRight mustBe true
        completionList.getRight.getItems.asScala.length mustEqual 1
        completionList.getRight.getItems.asScala.head.getDetail mustEqual
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

    "successfully change everythingOneError.riddl, fixing the error" in new ChangeOneErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      requestCompletion()

      completionResultF.asScala.failed.futureValue mustBe a[Throwable]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
    }

    "successfully change empty.riddl, expecting an error" in new ChangeEmptyFileSpec
      with CompletionRequestSpec {
      position.setLine(errorLine)
      position.setCharacter(errorLineChar)
      requestCompletion()

      whenReady(completionResultF.asScala) { completionList =>
        completionList.isRight mustBe true
        completionList.getRight.getItems.asScala.length mustEqual 1
        completionList.getRight.getItems.asScala.head.getDetail mustEqual
          """Expected one of (end-of-input | whitespace after keyword)"""
      }
    }

    "successfully save empty.riddl" in new OpenEmptyFileSpec
      with CompletionRequestSpec
      with DiagnosticRequestSpec {

      // ensuring state before the test
      position.setLine(1)
      position.setCharacter(1)
      requestCompletion()
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is empty"

      // Need to actually change the file before running the test
      val textChange: String =
        """domain New {}""".stripMargin

      var p = new java.io.PrintWriter(new File(emptyDocURI))
      try { p.println(textChange) }
      finally { p.close() }

      val saveNotification = new DidSaveTextDocumentParams()
      saveNotification.setTextDocument(textDocumentIdentifier)
      service.didSave(saveNotification)

      position.setLine(1)
      position.setCharacter(13)
      requestCompletion()

      whenReady(completionResultF.asScala) { completion =>
        completion.isRight mustEqual true
        completion.getRight.getItems.asScala.length mustEqual 1
        completion.getRight.getItems.asScala.head.getDetail mustEqual
          """Expected one of ("/*" | "//" | "???" | "application" | "author" | "by" | "command" | "constant" | "context" | "domain" | "epic" | "event" | "graph" | "import" | "include" | "option" | "query" | "record" | "result" | "saga" | "table" | "term" | "type" | "user")"""
      }

      p = new java.io.PrintWriter(new File(emptyDocURI))
      try { p.print("") }
      finally { p.close() }
    }
  }

}
