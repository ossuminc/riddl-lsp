package com.ossum.riddl.lsp.server

import org.eclipse.lsp4j.*

import scala.collection.immutable.List
import org.eclipse.lsp4j

import scala.jdk.CollectionConverters.*
import com.ossum.riddl.lsp
import com.ossum.riddl.lsp.utils.FileSpecs.*
import com.ossum.riddl.lsp.utils.{
  DocumentIdentifierSpec,
  FileSpecs,
  createTempFile
}
import com.ossuminc.riddl.lsp.utils.parsing.parseFromURI
import com.ossuminc.riddl.lsp.utils.parsing

import java.nio.file.Path

object InitializationSpecs {
  trait NoErrorInitializeSpec
      extends DocumentIdentifierSpec
      with NoErrorFileSpec {
    val noErrorDocURI: String = tempFilePath.toUri.toString
    val noErrorDoc: String = parseFromURI(noErrorDocURI)

    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText(noErrorDoc)
    textDocumentItem.setUri(noErrorDocURI)
    textDocumentIdentifier.setUri(noErrorDocURI)
  }

  trait OneErrorInitializeSpec
      extends DocumentIdentifierSpec
      with OneErrorFileSpec {
    val oneErrorDocURI: String = tempFilePath.toUri.toString
    val oneErrorDoc: String = parseFromURI(oneErrorDocURI)

    val textDocumentItem: TextDocumentItem = new TextDocumentItem()
    textDocumentItem.setText(oneErrorDoc)
    textDocumentItem.setUri(oneErrorDocURI)
    textDocumentIdentifier.setUri(oneErrorDocURI)
  }

  trait EmptyInitializeSpec extends DocumentIdentifierSpec {
    val resPath: String = "server/src/test/resources"

    val fileName = "empty.riddl"
    val tempFilePath: Path = createTempFile(fileName)

    val emptyDocURIFromPath: String = tempFilePath.toUri.toString

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
    versionedDocIdentifier.setUri(tempFilePath.toUri.toString)
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
