package com.ossum.riddl.lsp.server

import com.ossuminc.riddl.lsp.server.{RiddlLSPServer, RiddlLSPTextDocumentService, RiddlLSPWorkspaceService}
import com.ossuminc.riddl.lsp.utils.createInitializeResultIncremental
import org.eclipse.lsp4j
import org.eclipse.lsp4j.{DidChangeTextDocumentParams, DidCloseTextDocumentParams, DidOpenTextDocumentParams, DidSaveTextDocumentParams, InitializeParams, InitializeResult, Position, TextDocumentContentChangeEvent, TextDocumentIdentifier, TextDocumentItem, VersionedTextDocumentIdentifier}
import org.mockito.Mockito.mock
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Path
import scala.io.{BufferedSource, Source}
import scala.language.postfixOps
import scala.jdk.CollectionConverters.*

class RiddlLSPTextDocumentSpec extends AnyWordSpec with Matchers {
  val filePath = "server/src/test/resources/everything.riddl"
  val file: BufferedSource = Source.fromFile(filePath)
  val doc: String = file.getLines().mkString
  file.close()
  val docURI: String = Path.of(filePath).toUri.toString

  trait EverythingInitializeSpec {
    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText(doc)
    textDocumentItem.setUri(docURI)
    val textDocumentIdentifier: TextDocumentIdentifier = new TextDocumentIdentifier()
    textDocumentIdentifier.setUri(docURI)

    val service: RiddlLSPTextDocumentService = RiddlLSPTextDocumentService()
  }

  trait EmptyInitializeSpec {
    val emptyDocURI: String = Path.of("server/src/test/resources/empty.riddl").toUri.toString
    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText("")
    textDocumentItem.setUri(emptyDocURI)
    val textDocumentIdentifier: TextDocumentIdentifier = new TextDocumentIdentifier()
    textDocumentIdentifier.setUri(emptyDocURI)

    val service: RiddlLSPTextDocumentService = RiddlLSPTextDocumentService()
  }

  trait OpenEverythingFileSpec extends EverythingInitializeSpec {
    val openNotification: DidOpenTextDocumentParams = new DidOpenTextDocumentParams()
    openNotification.setTextDocument(
      textDocumentItem
    )

    service.didOpen(openNotification)
  }

  trait OpenEmptyFileSpec extends EmptyInitializeSpec {
    val openNotification: DidOpenTextDocumentParams = new DidOpenTextDocumentParams()
    openNotification.setTextDocument(
      textDocumentItem
    )

    service.didOpen(openNotification)
  }

  trait ChangeEverythingFileSpec extends OpenEverythingFileSpec {
    val changeNotification: DidChangeTextDocumentParams = new DidChangeTextDocumentParams()

    val versionedDocIdentifier = new VersionedTextDocumentIdentifier()
    versionedDocIdentifier.setUri(docURI)
    changeNotification.setTextDocument(versionedDocIdentifier)

    val changes = new TextDocumentContentChangeEvent()
    changes.setText("the")
    val changeRange = new lsp4j.Range()

    val changeStart = new Position()
    changeStart.setLine(11)
    changeStart.setCharacter(21)
    changeRange.setStart(changeStart)

    val changeEnd = new Position()
    changeEnd.setLine(11)
    changeEnd.setCharacter(24)
    changeRange.setEnd(changeEnd)

    changes.setRange(changeRange)
    changeNotification.setContentChanges(List(changes).asJava)

    service.didChange(changeNotification)
  }

  trait ChangeEmptyFileSpec extends OpenEmptyFileSpec {
    val changeNotification: DidChangeTextDocumentParams = new DidChangeTextDocumentParams()

    val versionedDocIdentifier = new VersionedTextDocumentIdentifier()
    versionedDocIdentifier.setUri(docURI)
    changeNotification.setTextDocument(versionedDocIdentifier)

    val changes = new TextDocumentContentChangeEvent()
    changes.setText("domain New {}")
    val changeRange = new lsp4j.Range()

    val changeStart = new Position()
    changeStart.setLine(1)
    changeStart.setCharacter(1)
    changeRange.setStart(changeStart)

    val changeEnd = new Position()
    changeEnd.setLine(1)
    changeEnd.setCharacter(1)
    changeRange.setEnd(changeEnd)

    changes.setRange(changeRange)
    changeNotification.setContentChanges(List(changes).asJava)

    service.didChange(changeNotification)
  }

  "RiddlLSPTextDocumentService" must {
    "successfully open everything.riddl" in new OpenEverythingFileSpec {}

    "successfully close everything.riddl" in new OpenEverythingFileSpec {
      val closeNotification: DidCloseTextDocumentParams = new DidCloseTextDocumentParams()
      closeNotification.setTextDocument(textDocumentIdentifier)
      service.didClose(closeNotification)
    }

    "successfully change everything.riddl" in new ChangeEverythingFileSpec { }

    "successfully save everything.riddl" in new ChangeEverythingFileSpec {
      val saveNotification = new DidSaveTextDocumentParams()
      saveNotification.setTextDocument(textDocumentIdentifier)
      service.didSave(saveNotification)
    }

    "successfully change empty.riddl" in new ChangeEmptyFileSpec {}

    "successfully save empty.riddl" in new ChangeEmptyFileSpec {
      val saveNotification = new DidSaveTextDocumentParams()
      saveNotification.setTextDocument(textDocumentIdentifier)
      service.didSave(saveNotification)
    }
  }

}
