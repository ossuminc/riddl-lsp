package com.ossum.riddl.lsp.server.LSPTextDocumentSpec.DocContentMgmtSpec

/*
- codeAction
- codeLens
- codeResolve
 */

class ActionsAndLensesSpec {

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
  
}
