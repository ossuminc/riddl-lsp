package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.DocContentMgmt

/*
- codeAction
- codeLens
- codeResolve
 */

object ActionsAndLenses {
  def diagnostic(
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
              val diagnostic: Diagnostic = new Diagnostic()
              diagnostic.setSeverity(msgToSeverity(msg))
              diagnostic.setMessage(msg.message)
              diagnostic.setRange(msg.toLSP4JRange)
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
