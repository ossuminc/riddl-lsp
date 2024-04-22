package com.ossum.riddl.lsp.server.LSPTextDocumentSpec.DocContentMgmtSpec

import com.ossum.riddl.lsp.server
import org.scalatest.ParallelTestExecution
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

/*
- formatting
- onTypeFormatting
- rangeFormatting
- rename
- prepareRename
 */

class FormattingAndRenamingSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with ParallelTestExecution {}
