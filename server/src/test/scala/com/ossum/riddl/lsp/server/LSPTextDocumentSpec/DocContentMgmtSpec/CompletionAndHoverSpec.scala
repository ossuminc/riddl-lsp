package com.ossum.riddl.lsp.server.LSPTextDocumentSpec.DocContentMgmtSpec

import com.ossum.riddl.lsp
import com.ossum.riddl.lsp.server.InitializationSpecs
import com.ossum.riddl.lsp.server.InitializationSpecs.{
  OpenEmptyFileSpec,
  OpenNoErrorFileSpec
}
import com.ossum.riddl.lsp.server.RequestSpecs.CompletionRequestSpec
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Files
import scala.jdk.FutureConverters.*

/*
- completion
- completionItemResolve
- hover
- signatureHelp
 */

class CompletionAndHoverSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures {
  "successfully open everything.riddl, failing to retrieve a completion" in new OpenNoErrorFileSpec
    with CompletionRequestSpec {
    position.setLine(1)
    position.setCharacter(1)
    requestCompletion()

    Files.delete(tempFilePath)

    completionResultF.asScala.failed.futureValue mustBe a[Throwable]
    completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document has no errors"
  }

  "successfully open empty.riddl, failing to retrieve a completion" in new OpenEmptyFileSpec
    with CompletionRequestSpec {

    position.setLine(1)
    position.setCharacter(1)
    requestCompletion()

    Files.delete(tempFilePath)

    completionResultF.asScala.failed.futureValue mustBe a[Throwable]
    completionResultF.asScala.failed.futureValue.getMessage mustEqual "Document is empty"
  }

}
