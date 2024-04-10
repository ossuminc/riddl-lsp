package com.ossum.riddl.lsp.server

import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService
import org.eclipse.lsp4j
import org.eclipse.lsp4j.jsonrpc.messages
import org.eclipse.lsp4j.{
  CompletionItem,
  CompletionList,
  CompletionParams,
  DidChangeTextDocumentParams,
  DidCloseTextDocumentParams,
  DidOpenTextDocumentParams,
  DidSaveTextDocumentParams,
  Position,
  TextDocumentContentChangeEvent,
  TextDocumentIdentifier,
  TextDocumentItem,
  VersionedTextDocumentIdentifier
}
import org.scalatest.Succeeded
import org.scalatest.concurrent.Futures.whenReady
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.nio.file.Path
import java.util
import java.util.concurrent.CompletableFuture
import scala.io.{BufferedSource, Source}
import scala.language.postfixOps
import scala.jdk.CollectionConverters.*
import scala.jdk.FutureConverters.*
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class RiddlLSPTextDocumentSpec
    extends AnyWordSpec
    with Matchers
    with ScalaFutures {

  trait OneErrorFileSpec {
    val filePath = "server/src/test/resources/everythingOneError.riddl"
    val file: BufferedSource = Source.fromFile(filePath)
    val doc: String = file.getLines().mkString
    file.close()
    val docURI: String = Path.of(filePath).toUri.toString
  }

  trait OneErrorInitializeSpec extends OneErrorFileSpec {

    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText(doc)
    textDocumentItem.setUri(docURI)
    val textDocumentIdentifier: TextDocumentIdentifier =
      new TextDocumentIdentifier()
    textDocumentIdentifier.setUri(docURI)

    val service: RiddlLSPTextDocumentService = RiddlLSPTextDocumentService()
  }

  trait EmptyInitializeSpec {
    val emptyDocURI: String =
      Path.of("server/src/test/resources/empty.riddl").toUri.toString

    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText("")
    textDocumentItem.setUri(emptyDocURI)
    val textDocumentIdentifier: TextDocumentIdentifier =
      new TextDocumentIdentifier()
    textDocumentIdentifier.setUri(emptyDocURI)

    val service: RiddlLSPTextDocumentService = RiddlLSPTextDocumentService()
  }

  trait OpenOneErrorFileSpec extends OneErrorInitializeSpec {
    val openNotification: DidOpenTextDocumentParams =
      new DidOpenTextDocumentParams()
    openNotification.setTextDocument(
      textDocumentItem
    )

    service.didOpen(openNotification)
  }

  trait OpenEmptyFileSpec extends EmptyInitializeSpec {
    val openNotification: DidOpenTextDocumentParams =
      new DidOpenTextDocumentParams()
    openNotification.setTextDocument(
      textDocumentItem
    )

    service.didOpen(openNotification)
  }

  trait ChangeOneErrorFileSpec extends OpenOneErrorFileSpec {
    val changeNotification: DidChangeTextDocumentParams =
      new DidChangeTextDocumentParams()

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

  trait ChangeEmptyFileSpec extends OpenEmptyFileSpec with OneErrorFileSpec {
    val changeNotification: DidChangeTextDocumentParams =
      new DidChangeTextDocumentParams()

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
    "successfully open everythingOneError.riddl & get completion" in new OpenOneErrorFileSpec {
      val params = new CompletionParams()
      params.setTextDocument(textDocumentIdentifier)
      val position = new Position()
      position.setLine(5)
      position.setCharacter(7)
      params.setPosition(position)

      val resultF: CompletableFuture[
        messages.Either[util.List[CompletionItem], CompletionList]
      ] = service.completion(params)

      whenReady(resultF.asScala) { eitherList =>
        eitherList.isRight mustBe true
        eitherList.getRight.getItems.asScala.length mustEqual 1
        eitherList.getRight.getItems.asScala.head.getDetail mustEqual
          """Expected one of (end-of-input | whitespace after keyword)"""
      }
    }

    /* TODO: Finish these tests immediately
    "successfully close everythingOneError.riddl" in new OpenOneErrorFileSpec {
      val closeNotification: DidCloseTextDocumentParams =
        new DidCloseTextDocumentParams()
      closeNotification.setTextDocument(textDocumentIdentifier)
      service.didClose(closeNotification)
    }

    "successfully change everythingOneError.riddl" in new ChangeOneErrorFileSpec {}

    "successfully save everythingOneError.riddl" in new ChangeOneErrorFileSpec {
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
     */
  }

}
