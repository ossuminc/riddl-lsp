package com.ossum.riddl.lsp

import com.ossuminc.riddl.lsp.server.RiddlLSPTextDocumentService
import org.eclipse.lsp4j.*

import java.nio.file.Path
import scala.collection.immutable.List
import scala.io.{BufferedSource, Source}

import org.eclipse.lsp4j
import scala.jdk.CollectionConverters.*

package server {

  import com.ossuminc.riddl.lsp.utils.parseFromURI

  object InitializationSpecs {
    trait DocumentIdentifierSpec {
      val textDocumentIdentifier: TextDocumentIdentifier =
        new TextDocumentIdentifier()

      val service: RiddlLSPTextDocumentService =
        new RiddlLSPTextDocumentService()
    }

    trait NonEmptyFileSpec {
      val filePath: String
    }

    trait NoErrorFileSpec extends NonEmptyFileSpec {
      override val filePath =
        "server/src/test/resources/everything.riddl"
    }

    trait OneErrorFileSpec extends NonEmptyFileSpec {
      override val filePath =
        "server/src/test/resources/everythingOneError.riddl"

      val errorLine = 5
      val errorCharOnLine = 7
    }

    trait NoErrorInitializeSpec
        extends NoErrorFileSpec
        with DocumentIdentifierSpec {
      val noErrorDocURI: String = Path.of(filePath).toUri.toString
      val noErrorDoc: String = parseFromURI(noErrorDocURI)

      val textDocumentItem: TextDocumentItem = new TextDocumentItem()
      textDocumentItem.setText(noErrorDoc)
      textDocumentItem.setUri(noErrorDocURI)
      textDocumentIdentifier.setUri(noErrorDocURI)
    }

    trait OneErrorInitializeSpec
        extends OneErrorFileSpec
        with DocumentIdentifierSpec {
      val oneErrorDocURI: String = Path.of(filePath).toUri.toString
      val oneErrorDoc: String = parseFromURI(oneErrorDocURI)

      val textDocumentItem: TextDocumentItem = new TextDocumentItem()
      textDocumentItem.setText(oneErrorDoc)
      textDocumentItem.setUri(oneErrorDocURI)
      textDocumentIdentifier.setUri(oneErrorDocURI)
    }

    trait EmptyInitializeSpec extends DocumentIdentifierSpec {
      val emptyDocURI = "server/src/test/resources/empty.riddl"

      val emptyDocURIFromPath: String =
        Path.of(emptyDocURI).toUri.toString

      val textDocumentItem: TextDocumentItem = new TextDocumentItem()
      textDocumentItem.setText("")
      textDocumentItem.setUri(emptyDocURIFromPath)
      textDocumentIdentifier.setUri(emptyDocURIFromPath)
    }

    trait OpenNoErrorFileSpec extends NoErrorInitializeSpec {
      val openNotification: DidOpenTextDocumentParams =
        new DidOpenTextDocumentParams()
      openNotification.setTextDocument(
        textDocumentItem
      )

      service.didOpen(openNotification)
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
      val textChange = "domain"

      val changeNotification: DidChangeTextDocumentParams =
        new DidChangeTextDocumentParams()

      val versionedDocIdentifier = new VersionedTextDocumentIdentifier()
      versionedDocIdentifier.setUri(oneErrorDocURI)
      changeNotification.setTextDocument(versionedDocIdentifier)

      val changes = new TextDocumentContentChangeEvent()
      changes.setText(textChange)
      val changeRange = new lsp4j.Range()

      val changeStart = new Position()
      changeStart.setLine(5)
      changeStart.setCharacter(1)
      changeRange.setStart(changeStart)

      val changeEnd = new Position()
      changeEnd.setLine(5)
      changeEnd.setCharacter(8)
      changeRange.setEnd(changeEnd)

      changes.setRange(changeRange)
      changeNotification.setContentChanges(List(changes).asJava)

      service.didChange(changeNotification)
    }

    trait ChangeEmptyFileSpec extends OpenEmptyFileSpec {
      val errorLine = 2
      val errorLineChar = 7

      val textChange: String =
        """domain New {
          |  typesss
          |}""".stripMargin

      val changeNotification: DidChangeTextDocumentParams =
        new DidChangeTextDocumentParams()

      val versionedDocIdentifier = new VersionedTextDocumentIdentifier()
      versionedDocIdentifier.setUri(emptyDocURI)
      changeNotification.setTextDocument(versionedDocIdentifier)

      val changes = new TextDocumentContentChangeEvent()
      changes.setText(textChange)
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
  }
}
