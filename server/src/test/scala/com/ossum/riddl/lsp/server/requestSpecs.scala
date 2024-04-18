package com.ossum.riddl.lsp.server

object requestSpecs {
  trait DiagnosticRequestSpec extends DocumentIdentifierSpec {
    val request = new DocumentDiagnosticParams(textDocumentIdentifier)

    val diagnosticResultF: CompletableFuture[DocumentDiagnosticReport] =
      service.diagnostic(request)
  }
}
