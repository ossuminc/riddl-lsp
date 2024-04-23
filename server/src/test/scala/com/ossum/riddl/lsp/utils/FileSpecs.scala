package com.ossum.riddl.lsp.utils

import com.ossum.riddl.lsp.utils.createTempFile
import com.ossuminc.riddl.lsp.server.RiddlLSPServer
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.services.TextDocumentService

import java.nio.file.Path

object FileSpecs {
  trait NonEmptyFileSpec {
    val fileName: String
    val resPath: String = "server/src/test/resources"
    val tempFilePath: Path
  }

  trait NoErrorFileSpec extends NonEmptyFileSpec {
    override val fileName = "everything.riddl"
    override val resPath = "server/src/test/resources"
    override val tempFilePath: Path = createTempFile(fileName)
  }

  trait OneErrorFileSpec extends NonEmptyFileSpec {
    override val fileName = "everythingOneError.riddl"
    override val tempFilePath: Path = createTempFile(fileName)

    val errorLine = 5
    val errorCharOnLine = 7
  }
}

trait DocumentIdentifierSpec {
  val textDocumentIdentifier: TextDocumentIdentifier =
    new TextDocumentIdentifier()

  val server: RiddlLSPServer = new RiddlLSPServer()
  val service: TextDocumentService = server.getTextDocumentService()
}
