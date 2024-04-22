package com.ossuminc.riddl.lsp.utils

import com.ossuminc.riddl.language.AST
import com.ossuminc.riddl.language.Messages.Messages
import com.ossuminc.riddl.language.parsing.{RiddlParserInput, TopLevelParser}
import parsing.parseFromURI
import scala.concurrent.ExecutionContext.Implicits.global

object parsing {
  def getRootFromUri(uri: String): String = {
    uri.split("/riddl/").drop(-1).mkString + "riddl/"
  }

  def parseDocFromSource(docURI: String): Option[Either[Messages, AST.Root]] = {
    val riddlRootDoc = java.net.URI.create(docURI).toURL

    // need to parse doc before giving to riddl parser to check if whole doc is empty (leads to parser failure)
    if parseFromURI(docURI).nonEmpty then
      Some(new TopLevelParser(RiddlParserInput(riddlRootDoc)).parseRoot())
    else None
  }

  def parseDocFromString(
      docString: String
  ): Option[Either[Messages, AST.Root]] =
    if docString.nonEmpty then
      Some(new TopLevelParser(RiddlParserInput(docString)).parseRoot())
    else None

  def parseFromURI(uri: String): String = {
    val source = io.Source.fromURL(uri)
    try {
      source.getLines().mkString("\n")
    } finally {
      source.close()
    }
  }
}
