package com.ossum.riddl.lsp.server.LSPTextDocumentSpec

import com.ossum.riddl.lsp
import com.ossum.riddl.lsp.server.*
import com.ossum.riddl.lsp.server.InitializationSpecs.*
import com.ossum.riddl.lsp.server.RequestSpecs.*
import com.ossum.riddl.lsp.utils.resetTempFile
import com.ossuminc.riddl.lsp.server.RiddlLSPServer
import org.eclipse.lsp4j
import org.eclipse.lsp4j.*
import org.scalatest.{BeforeAndAfterAll, ParallelTestExecution}
import org.scalatest.concurrent.Futures.whenReady
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*

class DocLifecycleMgmtSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with BeforeAndAfterAll {
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

      resetTempFile(tempFilePath, fileName)
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

      completionResultF.asScala.failed.futureValue mustBe a[Exception]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is closed"

      resetTempFile(tempFilePath, fileName)
    }

    "fail for completion error from a file with no errors" in new OpenNoErrorFileSpec
      with CompletionRequestSpec {
      val newFolder = "NoErrorFailCompletion"

      position.setLine(1)
      position.setCharacter(1)
      requestCompletion()

      completionResultF.asScala.failed.futureValue mustBe a[Exception]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"

      resetTempFile(tempFilePath, fileName)
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

      resetTempFile(tempFilePath, fileName)
    }

    "fail requesting for diagnostic from file with no errors" in new OpenNoErrorFileSpec
      with DiagnosticRequestSpec {

      diagnosticResultF.asScala.failed.futureValue mustBe a[Exception]
      diagnosticResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"

      resetTempFile(tempFilePath, fileName)
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

      resetTempFile(tempFilePath, fileName)
    }

    "successfully change everythingOneError.riddl, fixing the error" in new ChangeOneErrorFileSpec
      with CompletionRequestSpec {
      position.setLine(errorLine)
      position.setCharacter(errorCharOnLine)
      requestCompletion()

      completionResultF.asScala.failed.futureValue mustBe a[Exception]
      completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"

      resetTempFile(tempFilePath, fileName)
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

      resetTempFile(tempFilePath, fileName)
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

      val p = new java.io.PrintWriter(new File(tempFilePath.toString))
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

      resetTempFile(tempFilePath, fileName)
    }
  }

}
