package com.ossum.riddl.lsp.server.LSPTextDocumentSpec.DocContentMgmtSpec

import com.ossum.riddl.lsp.server.initializationSpecs.{
  DocumentIdentifierSpec,
  OpenNoErrorFileSpec
}
import org.eclipse.lsp4j.{
  CompletionItem,
  CompletionList,
  CompletionParams,
  DocumentDiagnosticParams,
  DocumentDiagnosticReport,
  Position
}
import org.eclipse.lsp4j.jsonrpc.messages

import java.util
import java.util.concurrent.CompletableFuture
import scala.concurrent.Future

/*
- completion
- completionItemResolve
- hover
- signatureHelp
 */

class CompletionAndHoverSpec {

  trait CompletionRequestSpec extends DocumentIdentifierSpec {

    var completionResultF: CompletableFuture[
      messages.Either[util.List[CompletionItem], CompletionList]
    ] = Future.failed(new Throwable()).asJava.toCompletableFuture

    def requestCompletion(): Unit = {
      params.setPosition(position)

      completionResultF = service.completion(params)
    }

    val params = new CompletionParams()
    params.setTextDocument(textDocumentIdentifier)
    val position = new Position()

  }

  "successfully open everything.riddl, failing to retrieve a completion" in new OpenNoErrorFileSpec
    with CompletionRequestSpec {
    position.setLine(1)
    position.setCharacter(1)
    requestCompletion()

    completionResultF.asScala.failed.futureValue mustBe a[Throwable]
    completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
  }

  "successfully open empty.riddl, failing to retrieve a completion" in new OpenEmptyFileSpec
    with CompletionRequestSpec {

    position.setLine(1)
    position.setCharacter(1)
    requestCompletion()

    completionResultF.asScala.failed.futureValue mustBe a[Throwable]
    completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is empty"
  }

}
