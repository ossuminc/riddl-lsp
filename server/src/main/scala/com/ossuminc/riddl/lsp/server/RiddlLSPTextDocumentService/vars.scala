package com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService

import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages

object vars {
  var docLines: Seq[String] = Seq()
  var docAST: Option[Either[Messages, AST.Root]] = None
  var docURI: Option[String] = None
  var riddlDoc: Option[String] = None
}
