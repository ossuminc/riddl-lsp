package com.ossum.riddl.lsp.server

import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService.RiddlLSPTextDocumentService
import com.ossuminc.riddl.lsp.server.{RiddlLSPServer, RiddlLSPWorkspaceService}
import com.ossuminc.riddl.lsp.utils.Utils.createInitializeResultIncremental
import org.eclipse.lsp4j.{InitializeParams, InitializeResult}
import org.scalatest.ParallelTestExecution
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.postfixOps

class RiddlLSPServerSpec
    extends AnyWordSpec
    with Matchers
    with ParallelTestExecution {

  trait InitializeSpec {
    val server: RiddlLSPServer = RiddlLSPServer()
    server.initialize(InitializeParams())
  }

  "RiddlLSPServer" must {
    "initialize properly" in {
      val server = RiddlLSPServer()
      val matchResult = createInitializeResultIncremental()
      server.initialize(InitializeParams()).get() mustBe matchResult
    }

    "shutdown properly" in new InitializeSpec {
      server.shutdown().get() mustBe null
    }

    "exit properly" in new InitializeSpec {
      server.exit() mustBe ()
    }

    "getTextDocumentService properly" in new InitializeSpec {
      server.getTextDocumentService() mustBe a[RiddlLSPTextDocumentService]
    }

    "getWorkspace properly" in new InitializeSpec {
      server.getWorkspaceService() mustBe a[RiddlLSPWorkspaceService]
    }
  }

}
