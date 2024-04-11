package com.ossuminc.riddl.lsp.utils

import com.ossuminc.riddl.language.Messages
import org.eclipse.lsp4j.{Location, Position, Range}

object implicits {
  implicit class TextProcessing(text: String) {
    def getLinesFromText: Seq[String] = text
      .replaceAll("(?<=\\S)(?= {2,})", "\n")
      .split("\\n")
      .toSeq
  }

  implicit class RIDDLMsg2LSP4JLoc(msg: Messages.Message) {
    def toLSP4JLocation: Location = {
      val location = new Location()
      val range = new Range()

      val startPosition = new Position()
      startPosition.setLine(msg.loc.line)
      startPosition.setCharacter(msg.loc.offset)
      val endPosition = new Position()
      endPosition.setLine(msg.loc.line)
      endPosition.setCharacter(msg.loc.offset + msg.context.length)

      range.setStart(startPosition)
      range.setEnd(endPosition)
      location.setRange(range)

      location
    }
  }
}
