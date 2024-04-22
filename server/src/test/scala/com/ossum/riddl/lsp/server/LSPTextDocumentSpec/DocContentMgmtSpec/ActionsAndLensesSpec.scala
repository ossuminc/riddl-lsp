package com.ossum.riddl.lsp.server.LSPTextDocumentSpec.DocContentMgmtSpec

import com.ossum.riddl.lsp
import com.ossum.riddl.lsp.server.InitializationSpecs.{
  OpenNoErrorFileSpec,
  OpenOneErrorFileSpec
}
import com.ossum.riddl.lsp.server.RequestSpecs.DiagnosticRequestSpec
import com.ossuminc.riddl.lsp.server.RiddlLSPServer
import org.eclipse.lsp4j.DocumentDiagnosticReportKind
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.io.File
import java.nio.file.Files
import scala.jdk.FutureConverters.*
import scala.jdk.CollectionConverters.*

/*
- codeAction
- codeLens
- codeResolve
 */

class ActionsAndLensesSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ParallelTestExecution {

  "fail requesting for diagnostic from file with no errors" in new OpenNoErrorFileSpec
    with DiagnosticRequestSpec {

    Files.delete(tempFilePath)

    diagnosticResultF.asScala.failed.futureValue mustBe a[Throwable]
    diagnosticResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
  }

  "succeed requesting for diagnostic from file with one error" in new OpenOneErrorFileSpec
    with DiagnosticRequestSpec {

    Files.delete(tempFilePath)

    whenReady(diagnosticResultF.asScala) { report =>
      report.getRelatedFullDocumentDiagnosticReport.getKind mustEqual DocumentDiagnosticReportKind.Full
      report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.length mustEqual 1
      report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getMessage mustEqual "Expected one of (end-of-input | whitespace after keyword)"
      report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getRange.getStart.getLine mustEqual errorLine
      report.getRelatedFullDocumentDiagnosticReport.getItems.asScala.head.getRange.getStart.getCharacter mustEqual errorCharOnLine
    }
  }

}
